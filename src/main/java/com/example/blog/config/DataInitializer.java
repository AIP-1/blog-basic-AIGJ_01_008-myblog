package com.example.blog.config;

import com.example.blog.domain.Category;
import com.example.blog.domain.Post;
import com.example.blog.domain.User;
import com.example.blog.repository.PostRepository;
import com.example.blog.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * 처음 실행할 때 관리자 계정과 resources/posts/*.md 강좌 글을 DB 에 넣는다.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final String ADMIN_USERNAME = "admin";

    private final UserService userService;
    private final PostRepository postRepository;
    private final String adminPassword;

    public DataInitializer(UserService userService, PostRepository postRepository,
                           @Value("${blog.admin-password}") String adminPassword) {
        this.userService = userService;
        this.postRepository = postRepository;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(String... args) throws IOException {
        User admin = userService.exists(ADMIN_USERNAME)
                ? userService.get(ADMIN_USERNAME)
                : userService.register(ADMIN_USERNAME, adminPassword, "ADMIN");

        if (postRepository.count() > 0) {
            return;
        }

        Resource[] files = new PathMatchingResourcePatternResolver().getResources("classpath:posts/*.md");
        Arrays.sort(files, Comparator.comparing(Resource::getFilename));
        for (Resource file : files) {
            String raw = file.getContentAsString(StandardCharsets.UTF_8);
            postRepository.save(parse(raw, admin));
        }
        log.info("강좌 글 {}개를 등록했습니다. 관리자 아이디: {}", files.length, ADMIN_USERNAME);
    }

    /**
     * 파일 형식:
     * <pre>
     * ---
     * title: 제목
     * category: BASIC
     * seq: 1
     * ---
     * 본문(마크다운)
     * </pre>
     */
    private Post parse(String raw, User author) {
        String text = raw.replace("\r\n", "\n");
        int end = text.indexOf("\n---", 3);
        Map<String, String> meta = new HashMap<>();
        for (String line : text.substring(3, end).trim().split("\n")) {
            int colon = line.indexOf(':');
            meta.put(line.substring(0, colon).trim(), line.substring(colon + 1).trim());
        }
        String body = text.substring(end + 4).trim();
        return new Post(
                meta.get("title"),
                body,
                Category.valueOf(meta.get("category")),
                Integer.valueOf(meta.get("seq")),
                author);
    }
}
