package com.example.blog.service;

import com.example.blog.domain.BlogSettings;
import com.example.blog.repository.BlogSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BlogSettingsService {

    public static final String DEFAULT_TITLE = "Java 블로그";
    public static final String DEFAULT_DESCRIPTION = "Java 입문·중급 강좌와 브라우저에서 바로 돌려보는 코드 실행기가 있는 블로그입니다.";

    private final BlogSettingsRepository repository;

    public BlogSettingsService(BlogSettingsRepository repository) {
        this.repository = repository;
    }

    public BlogSettings get() {
        return repository.findById(BlogSettings.SINGLETON_ID)
                .orElseGet(() -> new BlogSettings(DEFAULT_TITLE, DEFAULT_DESCRIPTION));
    }

    @Transactional
    public void update(String title, String description) {
        BlogSettings settings = repository.findById(BlogSettings.SINGLETON_ID)
                .orElseGet(() -> repository.save(new BlogSettings(DEFAULT_TITLE, DEFAULT_DESCRIPTION)));
        settings.update(title.trim(), description.trim());
    }
}
