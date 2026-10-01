package com.example.blog.web;

import com.example.blog.service.PostService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/** 세션에 '이번 세션에 이미 센 글'을 담아 두고 PostService 에 조회를 기록한다 (BlogVisits 와 같은 방식) */
@Component
public class PostViews {

    private static final String SESSION_KEY = "viewedPosts";

    private final PostService postService;

    public PostViews(PostService postService) {
        this.postService = postService;
    }

    public void record(Long postId, Authentication auth, HttpSession session) {
        @SuppressWarnings("unchecked")
        Set<Long> viewed = (Set<Long>) session.getAttribute(SESSION_KEY);
        if (viewed == null) {
            viewed = new HashSet<>();
        }
        postService.recordView(postId, auth == null ? null : auth.getName(), viewed);
        session.setAttribute(SESSION_KEY, viewed);
    }
}
