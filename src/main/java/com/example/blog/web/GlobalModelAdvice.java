package com.example.blog.web;

import com.example.blog.domain.BlogSettings;
import com.example.blog.service.BlogSettingsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Map;

/**
 * 모든 화면에서 쓰는 값: 블로그 이름·소개, 절대 주소(공유 링크와 미리보기 태그는 전체 URL 이 필요하다).
 */
@ControllerAdvice
public class GlobalModelAdvice {

    private final BlogSettingsService blogSettingsService;

    public GlobalModelAdvice(BlogSettingsService blogSettingsService) {
        this.blogSettingsService = blogSettingsService;
    }

    @ModelAttribute("blog")
    public BlogSettings blog() {
        return blogSettingsService.get();
    }

    @ModelAttribute("currentUrl")
    public String currentUrl() {
        return ServletUriComponentsBuilder.fromCurrentRequestUri().replaceQuery(null).toUriString();
    }

    @ModelAttribute("ogImage")
    public String ogImage() {
        return ServletUriComponentsBuilder.fromCurrentContextPath().path("/img/og-default.png").toUriString();
    }

    /** 업로드 용량 초과는 컨트롤러에 도달하기 전에 발생하므로 여기서 처리 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> uploadTooLarge() {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(Map.of("error", "이미지는 5MB 이하만 올릴 수 있습니다."));
    }
}
