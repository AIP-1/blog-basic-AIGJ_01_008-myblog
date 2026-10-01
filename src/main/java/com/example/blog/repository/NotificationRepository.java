package com.example.blog.repository;

import com.example.blog.domain.Comment;
import com.example.blog.domain.Notification;
import com.example.blog.domain.Post;
import com.example.blog.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    long countByRecipientUsernameAndIsReadFalse(String username);

    List<Notification> findTop8ByRecipientUsernameOrderByCreatedAtDescIdDesc(String username);

    Page<Notification> findByRecipientUsernameOrderByCreatedAtDescIdDesc(String username, Pageable pageable);

    @Modifying
    @Query("update Notification n set n.isRead = true where n.recipient.username = :username and n.isRead = false")
    int markAllRead(@Param("username") String username);

    @Modifying
    @Query("delete from Notification n where n.comment = :comment")
    void deleteByComment(@Param("comment") Comment comment);

    /** 차단할 때: 차단한 사람이 받은, 차단당한 사람이 보낸 알림을 지운다 */
    @Modifying
    @Query("delete from Notification n where n.recipient = :recipient and n.actor = :actor")
    void deleteByRecipientAndActor(@Param("recipient") User recipient, @Param("actor") User actor);

    @Modifying
    @Query("delete from Notification n where n.post = :post")
    void deleteByPost(@Param("post") Post post);
}
