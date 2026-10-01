package com.example.blog.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;

/**
 * 글에 넣는 이미지를 업로드 폴더에 저장한다.
 * 파일 이름·Content-Type 은 위조할 수 있으므로 파일 앞부분(매직 넘버)으로 실제 이미지 형식을 판별한다.
 * SVG 는 스크립트를 담을 수 있어 받지 않는다.
 */
@Service
public class UploadService {

    public static final String URL_PREFIX = "/uploads/";
    public static final long MAX_BYTES = 5 * 1024 * 1024;

    private final Path uploadDir;

    public UploadService(@Value("${blog.upload-dir}") String uploadDir) throws IOException {
        this.uploadDir = Path.of(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(this.uploadDir);
    }

    public Path getUploadDir() {
        return uploadDir;
    }

    /** 저장 후 이미지 주소(/uploads/파일명)를 반환 */
    public String storeImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어 있습니다.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("이미지는 5MB 이하만 올릴 수 있습니다.");
        }
        byte[] head;
        try (InputStream in = file.getInputStream()) {
            head = in.readNBytes(12);
        }
        String extension = detectImageType(head);
        if (extension == null) {
            throw new IllegalArgumentException("PNG, JPG, GIF, WEBP 이미지만 올릴 수 있습니다.");
        }
        String fileName = UUID.randomUUID() + "." + extension;
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, uploadDir.resolve(fileName));
        }
        return URL_PREFIX + fileName;
    }

    static String detectImageType(byte[] h) {
        if (startsWith(h, 0x89, 'P', 'N', 'G')) {
            return "png";
        }
        if (startsWith(h, 0xFF, 0xD8, 0xFF)) {
            return "jpg";
        }
        if (startsWith(h, 'G', 'I', 'F', '8')) {
            return "gif";
        }
        if (startsWith(h, 'R', 'I', 'F', 'F') && h.length >= 12
                && Arrays.equals(Arrays.copyOfRange(h, 8, 12), new byte[]{'W', 'E', 'B', 'P'})) {
            return "webp";
        }
        return null;
    }

    private static boolean startsWith(byte[] data, int... prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if ((data[i] & 0xFF) != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}
