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

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    /** 공개 글 검색. categoryId 가 null 이면 전체, keyword 가 빈 문자열이면 검색 안 함 */
    @Query("""
            select p from Post p
            where p.status = com.example.blog.domain.PostStatus.PUBLIC
              and (:categoryId is null or p.category.id = :categoryId)
              and (:keyword = ''
                   or lower(p.title) like lower(concat('%', :keyword, '%'))
                   or lower(p.content) like lower(concat('%', :keyword, '%')))
            """)
    Page<Post> searchPublic(@Param("categoryId") Long categoryId,
                            @Param("keyword") String keyword,
                            Pageable pageable);

    /** 관리 페이지 목록. username 이 null 이면 모든 사람의 글(관리자), status 가 null 이면 전체 상태 */
    @Query("""
            select p from Post p
            where (:username is null or p.author.username = :username)
              and (:status is null or p.status = :status)
            """)
    Page<Post> findForManage(@Param("username") String username,
                             @Param("status") PostStatus status,
                             Pageable pageable);

    @Query("""
            select count(p) from Post p
            where (:username is null or p.author.username = :username)
              and (:status is null or p.status = :status)
            """)
    long countForManage(@Param("username") String username, @Param("status") PostStatus status);

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
