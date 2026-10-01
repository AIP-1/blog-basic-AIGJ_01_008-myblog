package com.example.blog.web;

import com.example.blog.service.BlogService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/** 세션에 '이번 세션에 이미 센 블로그'를 담아 두고 BlogService 에 방문을 기록한다 */
@Component
public class BlogVisits {

    private static final String SESSION_KEY = "visitedBlogs";

    private final BlogService blogService;

    public BlogVisits(BlogService blogService) {
        this.blogService = blogService;
    }

    public void record(String blogOwner, Authentication auth, HttpSession session) {
        @SuppressWarnings("unchecked")
        Set<String> visited = (Set<String>) session.getAttribute(SESSION_KEY);
        if (visited == null) {
            visited = new HashSet<>();
        }
        blogService.recordVisit(blogOwner, auth == null ? null : auth.getName(), visited);
        // 세션 복제/저장소가 바뀐 값을 알아채도록 매번 다시 넣는다
        session.setAttribute(SESSION_KEY, visited);
    }
}
