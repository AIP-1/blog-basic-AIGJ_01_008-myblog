package com.example.blog.repository;

import com.example.blog.domain.Category;
import com.example.blog.domain.Post;
import com.example.blog.domain.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    /**
     * 공개 글 목록·검색. categoryId 가 null 이면 전체, keyword 가 빈 문자열이면 검색 안 함.
     * 검색할 때는 작성자 아이디 → 제목 → 내용 순으로 일치한 글을 먼저 보여 주고, 같은 순위 안에서는 최신순.
     * keyword 는 소문자로 넘긴다. 정렬이 쿼리에 들어 있으므로 Pageable 에는 정렬을 넣지 않는다.
     * viewer(로그인 아이디, 비로그인은 빈 문자열)가 차단한 사람의 글은 빼고 보여 준다.
     * from/to 가 있으면 그 기간에 쓴 글만 (달력에서 날짜를 눌렀을 때).
     */
    @Query(value = """
            select p from Post p
            where p.status = com.example.blog.domain.PostStatus.PUBLIC
              and (:categoryId is null or p.category.id = :categoryId)
              and (:keyword = ''
                   or lower(p.author.username) like concat('%', :keyword, '%')
                   or lower(p.title) like concat('%', :keyword, '%')
                   or lower(p.content) like concat('%', :keyword, '%'))
              and not exists (select 1 from UserBlock b
                              where b.blocker.username = :viewer and b.blocked = p.author)
              and (:from is null or p.createdAt >= :from)
              and (:to is null or p.createdAt < :to)
            order by
              case
                when :keyword = '' then 0
                when lower(p.author.username) like concat('%', :keyword, '%') then 0
                when lower(p.title) like concat('%', :keyword, '%') then 1
                else 2
              end,
              p.createdAt desc, p.id desc
            """,
            countQuery = """
            select count(p) from Post p
            where p.status = com.example.blog.domain.PostStatus.PUBLIC
              and (:categoryId is null or p.category.id = :categoryId)
              and (:keyword = ''
                   or lower(p.author.username) like concat('%', :keyword, '%')
                   or lower(p.title) like concat('%', :keyword, '%')
                   or lower(p.content) like concat('%', :keyword, '%'))
              and not exists (select 1 from UserBlock b
                              where b.blocker.username = :viewer and b.blocked = p.author)
              and (:from is null or p.createdAt >= :from)
              and (:to is null or p.createdAt < :to)
            """)
    Page<Post> searchPublic(@Param("categoryId") Long categoryId,
                            @Param("keyword") String keyword,
                            @Param("viewer") String viewer,
                            @Param("from") LocalDateTime from,
                            @Param("to") LocalDateTime to,
                            Pageable pageable);

    /**
     * 개인 블로그: 한 회원의 공개 글. categoryId 가 null 이면 전체, 0 이면 미분류, keyword(소문자)가 빈 문자열이면 검색 안 함.
     * 검색할 때는 제목 → 내용 순으로 일치한 글을 먼저, 같은 순위 안에서는 최신순.
     */
    @Query(value = """
            select p from Post p
            where p.status = com.example.blog.domain.PostStatus.PUBLIC
              and p.author.username = :username
              and (:categoryId is null
                   or (:categoryId = 0 and p.category is null)
                   or p.category.id = :categoryId)
              and (:keyword = ''
                   or lower(p.title) like concat('%', :keyword, '%')
                   or lower(p.content) like concat('%', :keyword, '%'))
            order by
              case
                when :keyword = '' then 0
                when lower(p.title) like concat('%', :keyword, '%') then 0
                else 1
              end,
              p.createdAt desc, p.id desc
            """,
            countQuery = """
            select count(p) from Post p
            where p.status = com.example.blog.domain.PostStatus.PUBLIC
              and p.author.username = :username
              and (:categoryId is null
                   or (:categoryId = 0 and p.category is null)
                   or p.category.id = :categoryId)
              and (:keyword = ''
                   or lower(p.title) like concat('%', :keyword, '%')
                   or lower(p.content) like concat('%', :keyword, '%'))
            """)
    Page<Post> findPublicByAuthor(@Param("username") String username,
                                  @Param("categoryId") Long categoryId,
                                  @Param("keyword") String keyword,
                                  Pageable pageable);

    /** 구독 피드: 구독한 블로그들의 공개 글 */
    @Query("""
            select p from Post p
            where p.status = com.example.blog.domain.PostStatus.PUBLIC
              and p.author in (select s.blogOwner from Subscription s where s.subscriber.username = :username)
            """)
    Page<Post> findSubscribedFeed(@Param("username") String username, Pageable pageable);

    /** 회원 블로그 순위 (공개 글이 하나라도 있는 회원, 방문 많은 순 → 같으면 최근 글 순) */
    @Query("""
            select new com.example.blog.repository.BlogSummary(p.author, count(p), max(p.createdAt))
            from Post p
            where p.status = com.example.blog.domain.PostStatus.PUBLIC
            group by p.author
            order by p.author.blogVisits desc, max(p.createdAt) desc
            """)
    List<BlogSummary> findBlogSummaries();

    long countByAuthorUsernameAndStatus(String username, PostStatus status);

    /** 관리 통계: 최근 글 */
    List<Post> findTop5ByAuthorUsernameOrderByCreatedAtDesc(String username);

    /** 관리 통계: 내 글이 받은 좋아요 수 */
    @Query("select count(l) from PostLike l where l.post.author.username = :username")
    long countLikesReceived(@Param("username") String username);

    /** 관리 통계: 내 글에 다른 사람이 단 댓글·답글 수 (삭제 표시된 것 제외) */
    @Query("""
            select count(c) from Comment c
            where c.post.author.username = :username and c.deleted = false and c.author.username <> :username
            """)
    long countCommentsReceived(@Param("username") String username);

    /** 달력: 기간 안에 쓴 공개 글의 작성 시각 */
    @Query("""
            select p.createdAt from Post p
            where p.status = com.example.blog.domain.PostStatus.PUBLIC
              and p.createdAt >= :from and p.createdAt < :to
            """)
    List<LocalDateTime> findPublicCreatedAtBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** 한 회원의 공개 글 수를 카테고리별로: [카테고리 id(미분류면 null), 글 수] */
    @Query("""
            select c.id, count(p) from Post p left join p.category c
            where p.status = com.example.blog.domain.PostStatus.PUBLIC and p.author.username = :username
            group by c.id
            """)
    List<Object[]> countPublicByCategory(@Param("username") String username);

    /** 사이드바 공지사항 (최신순) */
    List<Post> findTop5ByNoticeTrueAndStatusOrderByCreatedAtDesc(PostStatus status);

    /** 인기 글: 조회수 많은 순 → 같으면 최신순 */
    List<Post> findTop10ByStatusOrderByViewCountDescCreatedAtDesc(PostStatus status);

    /**
     * 관리 페이지 목록. username 이 null 이면 모든 사람의 글(관리자), status 가 null 이면 전체 상태,
     * keyword(소문자)가 빈 문자열이 아니면 제목·내용·작성자 아이디에서 찾는다.
     */
    @Query("""
            select p from Post p
            where (:username is null or p.author.username = :username)
              and (:status is null or p.status = :status)
              and (:keyword = ''
                   or lower(p.title) like concat('%', :keyword, '%')
                   or lower(p.content) like concat('%', :keyword, '%')
                   or lower(p.author.username) like concat('%', :keyword, '%'))
            """)
    Page<Post> findForManage(@Param("username") String username,
                             @Param("status") PostStatus status,
                             @Param("keyword") String keyword,
                             Pageable pageable);

    @Query("""
            select count(p) from Post p
            where (:username is null or p.author.username = :username)
              and (:status is null or p.status = :status)
              and (:keyword = ''
                   or lower(p.title) like concat('%', :keyword, '%')
                   or lower(p.content) like concat('%', :keyword, '%')
                   or lower(p.author.username) like concat('%', :keyword, '%'))
            """)
    long countForManage(@Param("username") String username, @Param("status") PostStatus status,
                        @Param("keyword") String keyword);

    List<Post> findByAuthorUsernameAndStatusOrderByUpdatedAtDesc(String username, PostStatus status);

    long countByCategory(Category category);

    @Modifying
    @Query("update Post p set p.category = null where p.category = :category")
    void clearCategory(@Param("category") Category category);

    /** 강좌 목차 (공개 글만) */
    List<Post> findBySeqNotNullAndStatusOrderBySeqAsc(PostStatus status);

    Optional<Post> findFirstBySeqLessThanAndStatusOrderBySeqDesc(Integer seq, PostStatus status);

    Optional<Post> findFirstBySeqGreaterThanAndStatusOrderBySeqAsc(Integer seq, PostStatus status);
}
