package com.example.blog.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User author;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected Comment() {
    }

    public Comment(String content, Post post, User author) {
        this.content = content;
        this.post = post;
        this.author = author;
        this.createdAt = LocalDateTime.now();
    }

    public boolean isWrittenBy(String username) {
        return author.getUsername().equals(username);
    }

    public Long getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public Post getPost() {
        return post;
    }

    public User getAuthor() {
        return author;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
