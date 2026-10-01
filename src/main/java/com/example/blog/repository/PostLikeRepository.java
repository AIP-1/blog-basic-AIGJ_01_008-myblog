package com.example.blog.repository;

import com.example.blog.domain.Post;
import com.example.blog.domain.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    Optional<PostLike> findByPostAndUserUsername(Post post, String username);

    boolean existsByPostAndUserUsername(Post post, String username);

    long countByPost(Post post);

    @Modifying
    @Query("delete from PostLike l where l.post = :post")
    void deleteByPost(@Param("post") Post post);
}
