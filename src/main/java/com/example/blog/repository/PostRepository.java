package com.example.blog.repository;

import com.example.blog.domain.Category;
import com.example.blog.domain.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    /** category 가 null 이면 전체, keyword 가 빈 문자열이면 검색 안 함 */
    @Query("""
            select p from Post p
            where (:category is null or p.category = :category)
              and (:keyword = ''
                   or lower(p.title) like lower(concat('%', :keyword, '%'))
                   or lower(p.content) like lower(concat('%', :keyword, '%')))
            """)
    Page<Post> search(@Param("category") Category category,
                      @Param("keyword") String keyword,
                      Pageable pageable);

    /** 강좌 목차 */
    List<Post> findBySeqNotNullOrderBySeqAsc();

    Optional<Post> findFirstBySeqLessThanOrderBySeqDesc(Integer seq);

    Optional<Post> findFirstBySeqGreaterThanOrderBySeqAsc(Integer seq);
}
