package com.example.blog.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    /** 마크다운 원문 */
    @Column(nullable = false, length = 100000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Category category;

    /** 강좌 순서 (강좌 글만 값이 있음, 일반 글은 null) */
    private Integer seq;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User author;

    @OneToMany(mappedBy = "post", cascade = CascadeType.REMOVE, orphanRemoval = true)
    @OrderBy("id asc")
    private List<Comment> comments = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected Post() {
    }

    public Post(String title, String content, Category category, Integer seq, User author) {
        this.title = title;
        this.content = content;
        this.category = category;
        this.seq = seq;
        this.author = author;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public void update(String title, String content, Category category) {
        this.title = title;
        this.content = content;
        this.category = category;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isWrittenBy(String username) {
        return author.getUsername().equals(username);
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public Category getCategory() {
        return category;
    }

    public Integer getSeq() {
        return seq;
    }

    public User getAuthor() {
        return author;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
