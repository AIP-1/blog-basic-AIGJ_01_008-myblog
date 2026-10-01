package com.example.blog.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** subscriber 가 blogOwner 의 개인 블로그를 구독한다 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"subscriber_id", "blog_owner_id"}))
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User subscriber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User blogOwner;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected Subscription() {
    }

    public Subscription(User subscriber, User blogOwner) {
        this.subscriber = subscriber;
        this.blogOwner = blogOwner;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public User getSubscriber() {
        return subscriber;
    }

    public User getBlogOwner() {
        return blogOwner;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
