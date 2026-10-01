package com.example.blog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/** 블로그 이름·소개. 행은 항상 하나(id = 1)만 있다. */
@Entity
public class BlogSettings {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id = SINGLETON_ID;

    @Column(nullable = false, length = 50)
    private String title;

    @Column(nullable = false, length = 300)
    private String description;

    protected BlogSettings() {
    }

    public BlogSettings(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public void update(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}
