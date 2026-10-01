package com.example.blog.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 댓글. parent 가 있으면 답글이다 (한 단계만: 답글의 답글도 원래 댓글 아래에 붙는다).
 * 답글이 달린 댓글을 지우면 내용만 지우고 '삭제된 댓글'로 남겨 답글이 사라지지 않게 한다.
 */
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

    @ManyToOne(fetch = FetchType.LAZY)
    private Comment parent;

    @OneToMany(mappedBy = "parent")
    @OrderBy("id asc")
    private List<Comment> replies = new ArrayList<>();

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean deleted;

    protected Comment() {
    }

    public Comment(String content, Post post, User author) {
        this(content, post, author, null);
    }

    public Comment(String content, Post post, User author, Comment parent) {
        this.content = content;
        this.post = post;
        this.author = author;
        this.parent = parent;
        this.createdAt = LocalDateTime.now();
    }

    public void markDeleted() {
        this.deleted = true;
        this.content = "";
    }

    public boolean isReply() {
        return parent != null;
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

    public Comment getParent() {
        return parent;
    }

    public List<Comment> getReplies() {
        return replies;
    }

    public boolean isDeleted() {
        return deleted;
    }
}
