package com.example.blog.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * recipient 에게 가는 알림.
 * COMMENT·REPLY: actor 가 post 에 comment 를 남겼다 (내 글에 댓글 / 내 댓글에 답글)
 * NEW_POST: 구독한 블로그의 actor 가 새 글 post 를 올렸다 (comment 없음)
 */
@Entity
public class Notification {

    public enum Type {
        COMMENT("님이 내 글에 댓글을 남겼어요"),
        REPLY("님이 내 댓글에 답글을 남겼어요"),
        NEW_POST("님이 새 글을 올렸어요");

        private final String message;

        Type(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User recipient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User actor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Post post;

    /** NEW_POST 알림은 null */
    @ManyToOne(fetch = FetchType.LAZY)
    private Comment comment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Type type;

    @Column(nullable = false)
    private boolean isRead;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected Notification() {
    }

    public Notification(User recipient, Comment comment, Type type) {
        this.recipient = recipient;
        this.actor = comment.getAuthor();
        this.post = comment.getPost();
        this.comment = comment;
        this.type = type;
        this.createdAt = LocalDateTime.now();
    }

    /** 구독한 블로그의 새 글 알림 */
    public static Notification newPost(User recipient, Post post) {
        Notification n = new Notification();
        n.recipient = recipient;
        n.actor = post.getAuthor();
        n.post = post;
        n.type = Type.NEW_POST;
        n.createdAt = LocalDateTime.now();
        return n;
    }

    /** 알림을 눌렀을 때 갈 곳: 댓글 알림은 그 댓글, 새 글 알림은 글 */
    public String targetUrl() {
        return "/posts/" + post.getId() + (comment == null ? "" : "#comment-" + comment.getId());
    }

    public void markRead() {
        this.isRead = true;
    }

    public Long getId() {
        return id;
    }

    public User getRecipient() {
        return recipient;
    }

    public User getActor() {
        return actor;
    }

    public Post getPost() {
        return post;
    }

    public Comment getComment() {
        return comment;
    }

    public Type getType() {
        return type;
    }

    public boolean isRead() {
        return isRead;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
