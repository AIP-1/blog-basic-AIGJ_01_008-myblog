package com.example.blog.repository;

import com.example.blog.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByUsernameAndBannedTrue(String username);

    /** 회원 관리: 아이디 검색 (빈 문자열이면 전체), 최근 가입순 */
    Page<User> findByUsernameContainingIgnoreCaseOrderByCreatedAtDesc(String keyword, Pageable pageable);

    @Modifying
    @Query("update User u set u.blogVisits = u.blogVisits + 1 where u.username = :username")
    int increaseBlogVisits(@Param("username") String username);
}
