package com.example.blog.domain;

public enum Category {
    BASIC("Java 입문"),
    INTERMEDIATE("Java 중급"),
    FREE("자유");

    private final String label;

    Category(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
