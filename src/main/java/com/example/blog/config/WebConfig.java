package com.example.blog.config;

import com.example.blog.service.UploadService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final UploadService uploadService;

    public WebConfig(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    /** 업로드한 이미지를 /uploads/파일명 으로 제공 */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(UploadService.URL_PREFIX + "**")
                .addResourceLocations(uploadService.getUploadDir().toUri().toString());
    }
}
