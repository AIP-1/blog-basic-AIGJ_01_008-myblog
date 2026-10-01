package com.example.blog.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.Formula;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Post {

    public static final String UNCATEGORIZED = "미분류";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    /** 마크다운 원문 */
    @Column(nullable = false, length = 100000)
    private String content;

    /** 카테고리가 삭제되면 null (미분류) */
    @ManyToOne(fetch = FetchType.LAZY)
    private Category category;

    /** DB 에는 일반 문자열로 (H2 의 ENUM 타입이면 값이 늘 때 기존 DB 가 새 값을 거부한다) */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private PostStatus status;

    /** 강좌 순서 (강좌 글만 값이 있음, 일반 글은 null) */
    private Integer seq;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User author;

    @OneToMany(mappedBy = "post", cascade = CascadeType.REMOVE, orphanRemoval = true)
    @OrderBy("id asc")
    private List<Comment> comments = new ArrayList<>();

    /** 발행일 (임시저장 글은 마지막 저장 시각, 발행하는 순간 발행 시각으로 바뀜) */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /** 조회수 (인기 글 기준). 기존 행이 있어도 컬럼이 추가되도록 기본값 0 */
    @Column(nullable = false, columnDefinition = "bigint default 0")
    private long viewCount;

    /** 좋아요 수. 글을 불러올 때 함께 세므로 목록에서 글마다 따로 조회하지 않는다 */
    @Formula("(select count(*) from post_like l where l.post_id = id)")
    private long likeCount;

    /**
     * 구독자에게 새 글 알림을 보냈는지. 처음 공개될 때 한 번만 보낸다.
     * 이 기능 전에 있던 글은 알림 대상이 아니므로 기존 행은 true 로 채운다.
     */
    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean subscribersNotified;

    /** 관리자가 지정한 공지사항 (사이드바 공지 칸에 보인다) */
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean notice;

    protected Post() {
    }

    public Post(String title, String content, Category category, PostStatus status, Integer seq, User author) {
        this.title = title;
        this.content = content;
        this.category = category;
        this.status = status;
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

    public void changeStatus(PostStatus newStatus) {
        if (status == PostStatus.DRAFT && newStatus != PostStatus.DRAFT) {
            // 임시저장 글을 발행하면 발행 시각을 작성일로 삼는다
            this.createdAt = LocalDateTime.now();
            this.updatedAt = this.createdAt;
        }
        this.status = newStatus;
    }

    /** 공개 상태이고 아직 구독자 알림을 안 보냈으면 true 를 돌려주고 보낸 것으로 표시 */
    public boolean markSubscribersNotifiedIfPublic() {
        if (!isPublic() || subscribersNotified) {
            return false;
        }
        subscribersNotified = true;
        return true;
    }

    public void changeNotice(boolean notice) {
        this.notice = notice;
    }

    public void increaseViewCount() {
        this.viewCount++;
    }

    public boolean isWrittenBy(String username) {
        return author.getUsername().equals(username);
    }

    public boolean isPublic() {
        return status == PostStatus.PUBLIC;
    }

    public boolean isDraft() {
        return status == PostStatus.DRAFT;
    }

    public String getCategoryName() {
        return category == null ? UNCATEGORIZED : category.getName();
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

    public PostStatus getStatus() {
        return status;
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

    /** 답글을 뺀 원 댓글 (답글은 각 댓글의 replies 로) */
    public List<Comment> getTopComments() {
        return comments.stream().filter(c -> !c.isReply()).toList();
    }

    /** 삭제 표시된 댓글을 뺀 댓글·답글 수 */
    public long getCommentCount() {
        return comments.stream().filter(c -> !c.isDeleted()).count();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public long getViewCount() {
        return viewCount;
    }

    public long getLikeCount() {
        return likeCount;
    }

    public boolean isNotice() {
        return notice;
    }
}
