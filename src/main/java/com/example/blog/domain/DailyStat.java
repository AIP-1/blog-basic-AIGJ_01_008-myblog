package com.example.blog.domain;

import jakarta.persistence.*;

import java.time.LocalDate;

/** 한 회원 블로그의 하루치 통계: 블로그 방문 수, 글 조회 수 (관리 화면의 최근 7일 그래프) */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"owner_id", "stat_date"}))
public class DailyStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User owner;

    /** DB 에서는 stat_date ('day' 는 H2 예약어) */
    @Column(name = "stat_date", nullable = false)
    private LocalDate day;

    @Column(nullable = false)
    private long blogVisits;

    @Column(nullable = false)
    private long postViews;

    protected DailyStat() {
    }

    public DailyStat(User owner, LocalDate day, long blogVisits, long postViews) {
        this.owner = owner;
        this.day = day;
        this.blogVisits = blogVisits;
        this.postViews = postViews;
    }

    public Long getId() {
        return id;
    }

    public User getOwner() {
        return owner;
    }

    public LocalDate getDay() {
        return day;
    }

    public long getBlogVisits() {
        return blogVisits;
    }

    public long getPostViews() {
        return postViews;
    }
}
