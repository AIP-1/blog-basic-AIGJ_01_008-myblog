package com.example.blog.domain;

import jakarta.persistence.*;

/** 관리 페이지에서 추가·수정·삭제할 수 있는 카테고리 */
@Entity
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String name;

    /** 화면에 보이는 순서 (작을수록 위) */
    @Column(nullable = false)
    private int sortOrder;

    protected Category() {
    }

    public Category(String name, int sortOrder) {
        this.name = name;
        this.sortOrder = sortOrder;
    }

    public void rename(String name) {
        this.name = name;
    }

    public void changeOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
