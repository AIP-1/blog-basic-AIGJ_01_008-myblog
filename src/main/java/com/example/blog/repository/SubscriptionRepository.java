package com.example.blog.repository;

import com.example.blog.domain.Subscription;
import com.example.blog.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findBySubscriberAndBlogOwner(User subscriber, User blogOwner);

    boolean existsBySubscriberUsernameAndBlogOwnerUsername(String subscriber, String blogOwner);

    long countByBlogOwner(User blogOwner);

    List<Subscription> findBySubscriberUsernameOrderByCreatedAtDesc(String subscriber);
}
