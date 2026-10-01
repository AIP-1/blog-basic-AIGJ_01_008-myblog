package com.example.blog.service;

import com.example.blog.domain.User;
import com.example.blog.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole())
                .accountLocked(user.isBanned()) // 정지된 계정은 로그인 불가 (LockedException)
                .build();
    }

    public boolean exists(String username) {
        return userRepository.existsByUsername(username);
    }

    @Transactional
    public User register(String username, String rawPassword, String role) {
        return userRepository.save(new User(username, passwordEncoder.encode(rawPassword), role));
    }

    public boolean isBanned(String username) {
        return userRepository.existsByUsernameAndBannedTrue(username);
    }

    /** 회원 관리 목록 */
    public Page<User> list(String keyword, int page) {
        return userRepository.findByUsernameContainingIgnoreCaseOrderByCreatedAtDesc(
                keyword == null ? "" : keyword.trim(), PageRequest.of(Math.max(page, 0), 20));
    }

    /** 이용 정지. 관리자 계정은 정지할 수 없다 */
    @Transactional
    public void ban(String username, String reason) {
        User user = get(username);
        if (user.isAdmin()) {
            throw new IllegalArgumentException("관리자 계정은 정지할 수 없습니다.");
        }
        String r = reason == null ? "" : reason.trim();
        if (r.length() > 200) {
            throw new IllegalArgumentException("정지 사유는 200자 이하로 입력하세요.");
        }
        user.ban(r.isEmpty() ? null : r);
    }

    @Transactional
    public void unban(String username) {
        get(username).unban();
    }

    public User get(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));
    }
}
