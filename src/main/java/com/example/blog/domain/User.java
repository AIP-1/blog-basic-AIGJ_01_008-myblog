package com.example.blog.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 20)
    private String role;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    /** 개인 블로그 이름·소개. 비어 있으면 기본값을 보여 준다 */
    @Column(length = 50)
    private String blogTitle;

    @Column(length = 300)
    private String blogIntro;

    /** 개인 블로그 방문 수 (회원 블로그 순위 기준). 기존 행이 있어도 컬럼이 추가되도록 기본값 0 */
    @Column(nullable = false, columnDefinition = "bigint default 0")
    private long blogVisits;

    protected User() {
    }

    public User(String username, String encodedPassword, String role) {
        this.username = username;
        this.password = encodedPassword;
        this.role = role;
        this.createdAt = LocalDateTime.now();
    }

    public void updateBlog(String blogTitle, String blogIntro) {
        this.blogTitle = blogTitle;
        this.blogIntro = blogIntro;
    }

    public String getBlogTitle() {
        return blogTitle == null || blogTitle.isBlank() ? username + "의 블로그" : blogTitle;
    }

    public String getBlogIntro() {
        return blogIntro == null ? "" : blogIntro;
    }

    public long getBlogVisits() {
        return blogVisits;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getRole() {
        return role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
