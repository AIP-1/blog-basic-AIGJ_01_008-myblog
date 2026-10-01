package com.example.blog.web;

import com.example.blog.service.UploadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
public class UploadController {

    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    /** 에디터에서 끌어다 놓거나 붙여넣은 이미지 업로드 → {"url": "/uploads/..."} */
    @PostMapping("/api/uploads")
    public ResponseEntity<Map<String, String>> upload(@RequestParam("image") MultipartFile image) throws IOException {
        try {
            return ResponseEntity.ok(Map.of("url", uploadService.storeImage(image)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
