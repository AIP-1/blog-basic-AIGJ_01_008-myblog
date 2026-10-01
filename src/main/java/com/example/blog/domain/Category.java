package com.example.blog.domain;

import jakarta.persistence.*;

/** 관리 페이지에서 추가·수정·삭제할 수 있는 카테고리. owner 가 null 이면 공용(관리자), 있으면 그 회원의 개인 블로그 카테고리 */
@Entity
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String name;

    /** 화면에 보이는 순서 (작을수록 위) */
    @Column(nullable = false)
    private int sortOrder;

    /** 이름은 같은 owner 안에서만 겹치지 않으면 된다 (CategoryService 에서 검사) */
    @ManyToOne(fetch = FetchType.LAZY)
    private User owner;

    protected Category() {
    }

    public Category(String name, int sortOrder) {
        this(name, sortOrder, null);
    }

    public Category(String name, int sortOrder, User owner) {
        this.name = name;
        this.sortOrder = sortOrder;
        this.owner = owner;
    }

    public boolean isOwnedBy(String username) {
        return owner != null && owner.getUsername().equals(username);
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

    public User getOwner() {
        return owner;
    }
}
