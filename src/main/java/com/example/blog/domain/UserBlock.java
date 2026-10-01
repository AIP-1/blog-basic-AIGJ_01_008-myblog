package com.example.blog.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** blocker 가 blocked 를 차단했다 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"blocker_id", "blocked_id"}))
public class UserBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User blocker;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User blocked;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected UserBlock() {
    }

    public UserBlock(User blocker, User blocked) {
        this.blocker = blocker;
        this.blocked = blocked;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public User getBlocker() {
        return blocker;
    }

    public User getBlocked() {
        return blocked;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
