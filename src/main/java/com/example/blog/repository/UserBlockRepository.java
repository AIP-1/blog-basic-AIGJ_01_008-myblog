package com.example.blog.repository;

import com.example.blog.domain.User;
import com.example.blog.domain.UserBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {

    boolean existsByBlockerUsernameAndBlockedUsername(String blocker, String blocked);

    Optional<UserBlock> findByBlockerAndBlocked(User blocker, User blocked);

    List<UserBlock> findByBlockerUsernameOrderByCreatedAtDesc(String blocker);

    @Query("select b.blocked.id from UserBlock b where b.blocker.username = :username")
    Set<Long> findBlockedIds(@Param("username") String username);
}
