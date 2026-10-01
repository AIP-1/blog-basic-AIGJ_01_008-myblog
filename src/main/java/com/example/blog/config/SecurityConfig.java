package com.example.blog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/posts/new").authenticated()
                        .requestMatchers("/manage/categories/**", "/manage/settings/**", "/manage/posts/*/notice", "/manage/users/**").hasRole("ADMIN")
                        .requestMatchers("/", "/css/**", "/img/**", "/login", "/signup", "/error", "/playground", "/calendar").permitAll()
                        .requestMatchers(HttpMethod.GET, "/posts/{id}", "/uploads/**", "/blogs", "/blog/*").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", false)
                        // 정지된 계정(LockedException)은 따로 안내한다
                        .failureHandler((request, response, e) -> response.sendRedirect(request.getContextPath()
                                + (e instanceof LockedException ? "/login?banned" : "/login?error")))
                        .permitAll())
                .logout(logout -> logout.logoutSuccessUrl("/"));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
