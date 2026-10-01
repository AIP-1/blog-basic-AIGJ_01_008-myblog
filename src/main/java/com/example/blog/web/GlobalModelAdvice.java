package com.example.blog.web;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * 모든 화면에서 쓰는 절대 주소. 공유 링크와 미리보기(Open Graph) 태그는 전체 URL 이 필요하다.
 */
@ControllerAdvice
public class GlobalModelAdvice {

    @ModelAttribute("currentUrl")
    public String currentUrl() {
        return ServletUriComponentsBuilder.fromCurrentRequestUri().replaceQuery(null).toUriString();
    }

    @ModelAttribute("ogImage")
    public String ogImage() {
        return ServletUriComponentsBuilder.fromCurrentContextPath().path("/img/og-default.png").toUriString();
    }
}
