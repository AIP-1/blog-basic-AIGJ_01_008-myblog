package com.example.blog.config;

import com.example.blog.domain.BlogSettings;
import com.example.blog.domain.Category;
import com.example.blog.domain.Post;
import com.example.blog.domain.PostStatus;
import com.example.blog.domain.User;
import com.example.blog.repository.BlogSettingsRepository;
import com.example.blog.repository.CategoryRepository;
import com.example.blog.repository.PostRepository;
import com.example.blog.service.BlogSettingsService;
import com.example.blog.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 처음 실행할 때 관리자 계정, 기본 카테고리, 블로그 설정, resources/posts/*.md 강좌 글을 DB 에 넣는다.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final String ADMIN_USERNAME = "admin";
    private static final List<String> DEFAULT_CATEGORIES = List.of("Java 입문", "Java 중급", "자유");

    private final UserService userService;
    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;
    private final BlogSettingsRepository blogSettingsRepository;
    private final JdbcTemplate jdbcTemplate;
    private final String adminPassword;

    public DataInitializer(UserService userService, PostRepository postRepository,
                           CategoryRepository categoryRepository, BlogSettingsRepository blogSettingsRepository,
                           JdbcTemplate jdbcTemplate, @Value("${blog.admin-password}") String adminPassword) {
        this.userService = userService;
        this.postRepository = postRepository;
        this.categoryRepository = categoryRepository;
        this.blogSettingsRepository = blogSettingsRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(String... args) throws IOException {
        dropOldCategoryNameUnique();
        allowNotificationWithoutComment();

        User admin = userService.exists(ADMIN_USERNAME)
                ? userService.get(ADMIN_USERNAME)
                : userService.register(ADMIN_USERNAME, adminPassword, "ADMIN");

        if (!blogSettingsRepository.existsById(BlogSettings.SINGLETON_ID)) {
            blogSettingsRepository.save(new BlogSettings(BlogSettingsService.DEFAULT_TITLE, BlogSettingsService.DEFAULT_DESCRIPTION));
        }
        if (categoryRepository.countByOwnerIsNull() == 0) {
            for (int i = 0; i < DEFAULT_CATEGORIES.size(); i++) {
                categoryRepository.save(new Category(DEFAULT_CATEGORIES.get(i), i + 1));
            }
        }

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
     * 예전에는 카테고리 이름이 전체에서 유일했지만, 이제는 회원마다 같은 이름을 쓸 수 있다.
     * ddl-auto: update 는 제약을 지우지 않으므로 기존 DB 에 남은 name 단독 UNIQUE 제약을 직접 지운다.
     */
    private void dropOldCategoryNameUnique() {
        List<String> names = jdbcTemplate.queryForList("""
                select tc.constraint_name
                from information_schema.table_constraints tc
                join information_schema.key_column_usage k
                  on k.constraint_name = tc.constraint_name and k.table_name = tc.table_name
                where tc.table_name = 'CATEGORY' and tc.constraint_type = 'UNIQUE'
                group by tc.constraint_name
                having count(*) = 1 and max(k.column_name) = 'NAME'
                """, String.class);
        for (String name : names) {
            jdbcTemplate.execute("alter table category drop constraint \"" + name + "\"");
            log.info("카테고리 이름 UNIQUE 제약({})을 지웠습니다.", name);
        }
    }

    /**
     * 새 글 알림은 댓글이 없으므로 notification.comment_id 가 비어 있을 수 있어야 한다.
     * ddl-auto: update 는 NOT NULL 을 풀지 않으므로 기존 DB 에서 직접 푼다.
     */
    private void allowNotificationWithoutComment() {
        List<String> nullable = jdbcTemplate.queryForList("""
                select is_nullable from information_schema.columns
                where table_name = 'NOTIFICATION' and column_name = 'COMMENT_ID'
                """, String.class);
        if (nullable.contains("NO")) {
            jdbcTemplate.execute("alter table notification alter column comment_id set null");
            log.info("알림의 comment_id 를 비어 있어도 되도록 바꿨습니다.");
        }
    }

    /**
     * 파일 형식:
     * <pre>
     * ---
     * title: 제목
     * category: Java 입문
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
        Category category = categoryRepository.findByOwnerIsNullAndName(meta.get("category"))
                .orElseGet(() -> categoryRepository.save(new Category(meta.get("category"), (int) categoryRepository.countByOwnerIsNull() + 1)));
        return new Post(
                meta.get("title"),
                body,
                category,
                PostStatus.PUBLIC,
                Integer.valueOf(meta.get("seq")),
                author);
    }
}
