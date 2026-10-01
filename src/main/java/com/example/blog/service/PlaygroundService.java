package com.example.blog.service;

import org.springframework.stereotype.Service;

import javax.tools.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 사용자가 입력한 Java 코드를 컴파일해서 별도 JVM 프로세스로 실행한다.
 * <p>
 * 안전장치: 실행 시간 제한, 메모리 제한, 출력 길이 제한, 동시 실행 수 제한,
 * Security Manager(파일·네트워크·프로세스 실행 차단).
 * 로컬/학습용 수준의 격리이므로 공개 서비스로 운영하려면 Docker 등 컨테이너 격리를 추가해야 한다.
 */
@Service
public class PlaygroundService {

    public static final int MAX_CODE_LENGTH = 20_000;
    private static final long TIMEOUT_SECONDS = 5;
    private static final int MAX_OUTPUT_BYTES = 20_000;
    private static final int MAX_CONCURRENT_RUNS = 2;

    private static final Pattern PUBLIC_TYPE = Pattern.compile(
            "public\\s+(?:(?:final|abstract|sealed|non-sealed)\\s+)*(?:class|record|enum|interface)\\s+(\\w+)");
    private static final Pattern ANY_TYPE = Pattern.compile("\\b(?:class|record|interface|enum)\\s+\\w+");

    private static final String SNIPPET_HEADER = """
            import java.util.*;
            import java.util.function.*;
            import java.util.stream.*;
            import java.io.*;

            public class Main {
                public static void main(String[] args) throws Exception {
            """;
    private static final String SNIPPET_FOOTER = "\n    }\n}\n";
    private static final int SNIPPET_LINE_OFFSET = (int) SNIPPET_HEADER.lines().count();

    private final Semaphore slots = new Semaphore(MAX_CONCURRENT_RUNS);

    public record Result(String status, String output, String error, Integer exitCode, long elapsedMs) {

        static Result of(String status, String error) {
            return new Result(status, "", error, null, 0);
        }
    }

    private record Source(String className, String text, int lineOffset) {
    }

    public Result run(String code, String input) throws InterruptedException {
        if (code == null || code.isBlank()) {
            return Result.of("ERROR", "코드를 입력하세요.");
        }
        if (code.length() > MAX_CODE_LENGTH) {
            return Result.of("ERROR", "코드가 너무 깁니다. (최대 " + MAX_CODE_LENGTH + "자)");
        }
        if (!slots.tryAcquire(3, TimeUnit.SECONDS)) {
            return Result.of("BUSY", "다른 코드가 실행 중입니다. 잠시 후 다시 시도하세요.");
        }
        Path dir = null;
        try {
            dir = Files.createTempDirectory("playground-");
            Source source = prepare(code);

            String compileErrors = compile(source, dir);
            if (compileErrors != null) {
                return Result.of("COMPILE_ERROR", compileErrors);
            }

            String mainClass = findMainClass(dir, source.className());
            if (mainClass == null) {
                return Result.of("ERROR", "main 메서드를 찾을 수 없습니다.\n"
                        + "public static void main(String[] args) { ... } 를 추가하거나, class 없이 실행할 문장만 입력하세요.");
            }
            return execute(dir, mainClass, input == null ? "" : input);
        } catch (IOException e) {
            return Result.of("ERROR", "실행 중 오류가 발생했습니다: " + e.getMessage());
        } finally {
            slots.release();
            deleteQuietly(dir);
        }
    }

    /** class 선언이 없으면 main 메서드 안에 넣어서 실행할 수 있게 감싼다. */
    private Source prepare(String code) {
        if (!ANY_TYPE.matcher(code).find()) {
            return new Source("Main", SNIPPET_HEADER + code + SNIPPET_FOOTER, SNIPPET_LINE_OFFSET);
        }
        Matcher m = PUBLIC_TYPE.matcher(code);
        return new Source(m.find() ? m.group(1) : "Main", code, 0);
    }

    /** 성공하면 null, 실패하면 오류 메시지 */
    private String compile(Source source, Path dir) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IOException("Java 컴파일러를 찾을 수 없습니다. JRE 가 아닌 JDK 로 서버를 실행하세요.");
        }
        Path file = dir.resolve(source.className() + ".java");
        Files.writeString(file, source.text(), StandardCharsets.UTF_8);

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager fm = compiler.getStandardFileManager(diagnostics, Locale.getDefault(), StandardCharsets.UTF_8)) {
            List<String> options = List.of("-d", dir.toString(), "-encoding", "UTF-8", "-proc:none", "-nowarn");
            boolean ok = compiler.getTask(null, fm, diagnostics, options, null,
                    fm.getJavaFileObjects(file.toFile())).call();
            if (ok) {
                return null;
            }
        }
        return diagnostics.getDiagnostics().stream()
                .filter(d -> d.getKind() == Diagnostic.Kind.ERROR)
                .map(d -> {
                    long line = Math.max(1, d.getLineNumber() - source.lineOffset());
                    return line + "번째 줄: " + d.getMessage(Locale.getDefault());
                })
                .collect(Collectors.joining("\n"));
    }

    /**
     * 컴파일된 클래스 중 public static void main(String[]) 을 가진 클래스를 찾는다.
     * 클래스를 초기화하지 않고(static 블록 실행 X) 메서드 정보만 확인한다.
     */
    private String findMainClass(Path dir, String preferred) throws IOException {
        List<String> names;
        try (Stream<Path> files = Files.list(dir)) {
            names = files.map(p -> p.getFileName().toString())
                    .filter(n -> n.endsWith(".class"))
                    .map(n -> n.substring(0, n.length() - ".class".length()))
                    .sorted(Comparator.comparing((String n) -> !n.equals(preferred)).thenComparing(n -> n.contains("$")))
                    .toList();
        }
        try (URLClassLoader loader = new URLClassLoader(new URL[]{dir.toUri().toURL()}, null)) {
            for (String name : names) {
                try {
                    Class<?> type = Class.forName(name.replace('/', '.'), false, loader);
                    Method main = type.getMethod("main", String[].class);
                    if (Modifier.isStatic(main.getModifiers()) && main.getReturnType() == void.class) {
                        return type.getName();
                    }
                } catch (ReflectiveOperationException | LinkageError ignored) {
                    // main 이 없는 클래스
                }
            }
        }
        return null;
    }

    private Result execute(Path dir, String mainClass, String input) throws IOException, InterruptedException {
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        List<String> command = List.of(java,
                "-Xmx64m", "-Xss1m", "-XX:+UseSerialGC", "-XX:TieredStopAtLevel=1",
                "-Djava.security.manager=default",
                "-Dfile.encoding=UTF-8", "-Dsun.stdout.encoding=UTF-8", "-Dsun.stderr.encoding=UTF-8",
                "-cp", dir.toString(), mainClass);

        long start = System.nanoTime();
        Process process = new ProcessBuilder(command).directory(dir.toFile()).start();
        LimitedBuffer out = new LimitedBuffer();
        LimitedBuffer err = new LimitedBuffer();
        Thread outReader = drain(process.getInputStream(), out);
        Thread errReader = drain(process.getErrorStream(), err);

        try (OutputStream stdin = process.getOutputStream()) {
            stdin.write(input.getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) {
            // 입력을 다 읽기 전에 프로그램이 끝난 경우
        }

        boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            process.waitFor();
        }
        outReader.join(1000);
        errReader.join(1000);
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        String error = cleanStderr(err.text());
        if (!finished) {
            return new Result("TIMEOUT", out.text(),
                    (error.isEmpty() ? "" : error + "\n") + "실행 시간이 " + TIMEOUT_SECONDS + "초를 넘어 중단했습니다. 무한 루프가 없는지 확인하세요.",
                    null, elapsedMs);
        }
        int exitCode = process.exitValue();
        return new Result(exitCode == 0 ? "OK" : "RUNTIME_ERROR", out.text(), error, exitCode, elapsedMs);
    }

    /** JVM 이 출력하는 Security Manager 경고는 사용자 코드와 무관하므로 숨긴다. */
    private static String cleanStderr(String stderr) {
        return stderr.lines()
                .filter(line -> !(line.startsWith("WARNING:") && line.contains("Security Manager")))
                .collect(Collectors.joining("\n"));
    }

    private static Thread drain(InputStream in, LimitedBuffer sink) {
        Thread t = new Thread(() -> {
            byte[] buf = new byte[8192];
            try (in) {
                int n;
                while ((n = in.read(buf)) != -1) {
                    sink.write(buf, n);
                }
            } catch (IOException ignored) {
                // 프로세스 종료
            }
        });
        t.setDaemon(true);
        t.start();
        return t;
    }

    /** 일정 크기까지만 저장하고 나머지는 버린다 (무한 출력 대비). */
    private static class LimitedBuffer {
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private boolean truncated;

        synchronized void write(byte[] buf, int len) {
            int room = MAX_OUTPUT_BYTES - bytes.size();
            if (len > room) {
                truncated = true;
            }
            if (room > 0) {
                bytes.write(buf, 0, Math.min(len, room));
            }
        }

        synchronized String text() {
            String s = bytes.toString(StandardCharsets.UTF_8);
            return truncated ? s + "\n... (출력이 너무 길어 잘렸습니다)" : s;
        }
    }

    private static void deleteQuietly(Path dir) {
        if (dir == null) {
            return;
        }
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        } catch (IOException ignored) {
            // 임시 파일이라 실패해도 무시
        }
    }
}
