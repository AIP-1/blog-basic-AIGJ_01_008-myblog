package com.example.blog.config;

import com.example.blog.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.security.Principal;

/** 로그인한 채로 정지된 회원은 다음 요청에서 로그아웃시키고 로그인 화면으로 보낸다 */
@Component
public class BannedUserInterceptor implements HandlerInterceptor {

    private final UserService userService;

    public BannedUserInterceptor(UserService userService) {
        this.userService = userService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Principal principal = request.getUserPrincipal();
        if (principal != null && userService.isBanned(principal.getName())) {
            request.logout();
            response.sendRedirect(request.getContextPath() + "/login?banned");
            return false;
        }
        return true;
    }
}
