package com.example.blog.repository;

import com.example.blog.domain.User;

import java.time.LocalDateTime;

/** 회원 블로그 목록 한 줄: 주인, 공개 글 수, 마지막 글 작성일 */
public record BlogSummary(User owner, long postCount, LocalDateTime lastPostedAt) {
}
