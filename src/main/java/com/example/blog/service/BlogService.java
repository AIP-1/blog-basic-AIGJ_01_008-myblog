package com.example.blog.service;

import com.example.blog.domain.Post;
import com.example.blog.domain.PostStatus;
import com.example.blog.domain.Subscription;
import com.example.blog.domain.User;
import com.example.blog.repository.BlogCategory;
import com.example.blog.repository.BlogSummary;
import com.example.blog.repository.PostRepository;
import com.example.blog.repository.SubscriptionRepository;
import com.example.blog.repository.UserBlockRepository;
import com.example.blog.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** 회원 개인 블로그와 구독 */
@Service
@Transactional(readOnly = true)
public class BlogService {

    public static final int MAX_TITLE = 50;
    public static final int MAX_INTRO = 300;
    private static final int PAGE_SIZE = 10;

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final CategoryService categoryService;
    private final UserBlockRepository blockRepository;

    public BlogService(UserRepository userRepository, PostRepository postRepository,
                       SubscriptionRepository subscriptionRepository, CategoryService categoryService,
                       UserBlockRepository blockRepository) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.categoryService = categoryService;
        this.blockRepository = blockRepository;
    }

    public User owner(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "블로그를 찾을 수 없습니다."));
    }

    public List<BlogSummary> blogs() {
        return postRepository.findBlogSummaries();
    }

    /** 블로그 글 목록·검색. 목록은 최신순, 검색하면 제목 → 내용 일치 순 (정렬은 쿼리 안에 있음) */
    public Page<Post> posts(String username, Long categoryId, String keyword, int page) {
        String q = keyword == null ? "" : keyword.trim().toLowerCase();
        return postRepository.findPublicByAuthor(username, categoryId, q, PageRequest.of(Math.max(page, 0), PAGE_SIZE));
    }

    /**
     * 블로그 카테고리 목록: 개인 카테고리(글이 없어도) → 이 회원이 글을 쓴 공용 카테고리 → 미분류(글이 있을 때만).
     * 각 줄에 이 회원의 공개 글 수를 붙인다.
     */
    public List<BlogCategory> categories(String username) {
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : postRepository.countPublicByCategory(username)) {
            counts.put(row[0] == null ? BlogCategory.UNCATEGORIZED_ID : (Long) row[0], (Long) row[1]);
        }
        List<BlogCategory> result = new ArrayList<>();
        categoryService.listOf(username).forEach(c ->
                result.add(new BlogCategory(c.getId(), c.getName(), counts.getOrDefault(c.getId(), 0L), true)));
        categoryService.list().stream()
                .filter(c -> counts.containsKey(c.getId()))
                .forEach(c -> result.add(new BlogCategory(c.getId(), c.getName(), counts.get(c.getId()), false)));
        Long uncategorized = counts.get(BlogCategory.UNCATEGORIZED_ID);
        if (uncategorized != null) {
            result.add(new BlogCategory(BlogCategory.UNCATEGORIZED_ID, Post.UNCATEGORIZED, uncategorized, false));
        }
        return result;
    }

    /** 사이드바 '내 블로그' 카드: 블로그 주인, 공개 글 수, 구독자 수 */
    public record MyBlog(User owner, long postCount, long subscriberCount) {
    }

    /** 계정을 찾을 수 없으면(예: 로그인 중 탈퇴) 카드 없이 화면을 보여 주도록 empty */
    public Optional<MyBlog> myBlog(String username) {
        return userRepository.findByUsername(username)
                .map(owner -> new MyBlog(owner, publicPostCount(username), subscriberCount(owner)));
    }

    public long publicPostCount(String username) {
        return postRepository.countByAuthorUsernameAndStatus(username, PostStatus.PUBLIC);
    }

    @Transactional
    public void updateSettings(String username, String title, String intro) {
        String t = title == null ? "" : title.trim();
        String i = intro == null ? "" : intro.trim();
        if (t.length() > MAX_TITLE) {
            throw new IllegalArgumentException("블로그 이름은 " + MAX_TITLE + "자 이하로 입력하세요.");
        }
        if (i.length() > MAX_INTRO) {
            throw new IllegalArgumentException("소개는 " + MAX_INTRO + "자 이하로 입력하세요.");
        }
        owner(username).updateBlog(t.isEmpty() ? null : t, i.isEmpty() ? null : i);
    }

    /**
     * 블로그 방문 기록. 같은 세션에서는 블로그마다 한 번만 세고, 주인 본인의 방문은 세지 않는다.
     * visited 는 세션에 보관하는 '이미 센 블로그' 목록이다.
     */
    @Transactional
    public void recordVisit(String blogOwner, String viewer, Set<String> visited) {
        if (blogOwner.equals(viewer) || !visited.add(blogOwner)) {
            return;
        }
        if (userRepository.increaseBlogVisits(blogOwner) == 0) {
            visited.remove(blogOwner); // 없는 회원
        }
    }

    // ===== 구독 =====

    public boolean isSubscribed(String subscriber, String blogOwner) {
        return subscriber != null && subscriptionRepository.existsBySubscriberUsernameAndBlogOwnerUsername(subscriber, blogOwner);
    }

    public long subscriberCount(User blogOwner) {
        return subscriptionRepository.countByBlogOwner(blogOwner);
    }

    public List<Subscription> subscriptions(String subscriber) {
        return subscriptionRepository.findBySubscriberUsernameOrderByCreatedAtDesc(subscriber);
    }

    @Transactional
    public void subscribe(String subscriber, String blogOwner) {
        if (subscriber.equals(blogOwner)) {
            throw new IllegalArgumentException("내 블로그는 구독할 수 없습니다.");
        }
        User me = owner(subscriber);
        User target = owner(blogOwner);
        if (blockRepository.existsByBlockerUsernameAndBlockedUsername(blogOwner, subscriber)) {
            throw new IllegalArgumentException("구독할 수 없는 블로그입니다.");
        }
        if (blockRepository.existsByBlockerUsernameAndBlockedUsername(subscriber, blogOwner)) {
            throw new IllegalArgumentException("차단한 블로그는 구독할 수 없어요. 차단을 먼저 풀어 주세요.");
        }
        if (subscriptionRepository.findBySubscriberAndBlogOwner(me, target).isEmpty()) {
            subscriptionRepository.save(new Subscription(me, target));
        }
    }

    @Transactional
    public void unsubscribe(String subscriber, String blogOwner) {
        subscriptionRepository.findBySubscriberAndBlogOwner(owner(subscriber), owner(blogOwner))
                .ifPresent(subscriptionRepository::delete);
    }

    /** 구독한 블로그들의 공개 글, 최신순 */
    public Page<Post> feed(String subscriber, int page) {
        return postRepository.findSubscribedFeed(subscriber, pageable(page));
    }

    private PageRequest pageable(int page) {
        return PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
    }
}
