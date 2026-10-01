package com.example.blog.domain;

public enum PostStatus {
    PUBLIC("공개"),
    PRIVATE("비공개"),
    DRAFT("임시저장");

    private final String label;

    PostStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
