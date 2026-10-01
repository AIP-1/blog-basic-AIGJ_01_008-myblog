package com.example.blog;

import com.example.blog.service.PlaygroundService;
import com.example.blog.service.PlaygroundService.Result;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlaygroundServiceTests {

    private final PlaygroundService service = new PlaygroundService();

    @Test
    void 클래스를_작성하면_그대로_실행한다() throws Exception {
        Result r = service.run("""
                public class Hello {
                    public static void main(String[] args) {
                        System.out.println("안녕, 자바!");
                    }
                }
                """, "");
        assertThat(r.status()).isEqualTo("OK");
        assertThat(r.output()).isEqualTo("안녕, 자바!\n");
    }

    @Test
    void 문장만_입력하면_main_안에서_실행한다() throws Exception {
        Result r = service.run("List<Integer> nums = List.of(1, 2, 3);\nSystem.out.println(nums.stream().mapToInt(i -> i).sum());", "");
        assertThat(r.status()).isEqualTo("OK");
        assertThat(r.output().trim()).isEqualTo("6");
    }

    @Test
    void 표준입력을_전달한다() throws Exception {
        Result r = service.run("Scanner sc = new Scanner(System.in);\nSystem.out.println(sc.nextInt() * 2);", "21\n");
        assertThat(r.output().trim()).isEqualTo("42");
    }

    @Test
    void 컴파일_오류는_사용자_코드의_줄번호로_알려준다() throws Exception {
        Result r = service.run("int a = 1;\nint b = \"문자\";", "");
        assertThat(r.status()).isEqualTo("COMPILE_ERROR");
        assertThat(r.error()).startsWith("2번째 줄");
    }

    @Test
    void 런타임_예외를_보여준다() throws Exception {
        Result r = service.run("String s = null;\ns.length();", "");
        assertThat(r.status()).isEqualTo("RUNTIME_ERROR");
        assertThat(r.error()).contains("NullPointerException").doesNotContain("Security Manager");
    }

    @Test
    void 무한루프는_시간초과로_중단한다() throws Exception {
        Result r = service.run("while (true) {}", "");
        assertThat(r.status()).isEqualTo("TIMEOUT");
    }

    @Test
    void 파일_접근은_차단된다() throws Exception {
        Result r = service.run("System.out.println(java.nio.file.Files.readString(java.nio.file.Path.of(\"/etc/hosts\")));", "");
        assertThat(r.status()).isEqualTo("RUNTIME_ERROR");
        assertThat(r.error()).contains("AccessControlException");
    }

    @Test
    void main이_없으면_안내한다() throws Exception {
        Result r = service.run("class Person { String name; }", "");
        assertThat(r.status()).isEqualTo("ERROR");
        assertThat(r.error()).contains("main 메서드");
    }
}
