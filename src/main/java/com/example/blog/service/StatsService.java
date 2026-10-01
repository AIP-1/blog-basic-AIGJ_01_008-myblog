package com.example.blog.service;

import com.example.blog.domain.DailyStat;
import com.example.blog.domain.Post;
import com.example.blog.domain.PostStatus;
import com.example.blog.domain.User;
import com.example.blog.repository.DailyStatRepository;
import com.example.blog.repository.PostRepository;
import com.example.blog.repository.SubscriptionRepository;
import com.example.blog.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 블로그 관리 첫 화면(통계): 날짜별 방문·조회 기록과 요약 */
@Service
@Transactional(readOnly = true)
public class StatsService {

    public static final int DAYS = 7;

    private final DailyStatRepository dailyStatRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final Clock clock = Clock.systemDefaultZone();

    public StatsService(DailyStatRepository dailyStatRepository, UserRepository userRepository,
                        PostRepository postRepository, SubscriptionRepository subscriptionRepository) {
        this.dailyStatRepository = dailyStatRepository;
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    // ===== 기록 (방문·조회가 '세어졌을 때'만 불린다) =====

    @Transactional
    public void recordBlogVisit(String owner) {
        if (dailyStatRepository.increaseBlogVisits(owner, today()) == 0) {
            create(owner, 1, 0);
        }
    }

    @Transactional
    public void recordPostView(String author) {
        if (dailyStatRepository.increasePostViews(author, today()) == 0) {
            create(owner(author), 0, 1);
        }
    }

    private void create(String owner, long visits, long views) {
        create(owner(owner), visits, views);
    }

    private void create(User owner, long visits, long views) {
        if (owner != null) {
            dailyStatRepository.save(new DailyStat(owner, today(), visits, views));
        }
    }

    private User owner(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    // ===== 관리 화면 =====

    /** 하루치 숫자 */
    public record Day(LocalDate date, long blogVisits, long postViews) {

        /** 10/1 */
        public String label() {
            return date.getMonthValue() + "/" + date.getDayOfMonth();
        }

        /** 수 */
        public String weekday() {
            return date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.KOREAN);
        }

        /** 막대 높이 (가장 큰 날 = 100) */
        public long percentOf(long max) {
            return max == 0 ? 0 : blogVisits * 100 / max;
        }
    }

    /** 최근 글 한 줄 */
    public record RecentPost(Post post, long likes, long comments) {
    }

    public record Dashboard(User owner, List<Day> days, long maxVisits, long weekVisits, long weekViews,
                            long subscribers, long publicPosts, long likesReceived, long commentsReceived,
                            List<RecentPost> recentPosts) {

        public Day today() {
            return days.get(days.size() - 1);
        }

        public Day yesterday() {
            return days.get(days.size() - 2);
        }
    }

    public Dashboard dashboard(String username) {
        User owner = userRepository.findByUsername(username).orElseThrow();
        LocalDate to = today();
        LocalDate from = to.minusDays(DAYS - 1);
        Map<LocalDate, DailyStat> stats = dailyStatRepository.findByOwnerUsernameAndDayBetween(username, from, to)
                .stream().collect(Collectors.toMap(DailyStat::getDay, Function.identity()));
        List<Day> days = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            DailyStat s = stats.get(d);
            days.add(new Day(d, s == null ? 0 : s.getBlogVisits(), s == null ? 0 : s.getPostViews()));
        }
        List<RecentPost> recent = postRepository.findTop5ByAuthorUsernameOrderByCreatedAtDesc(username).stream()
                .map(p -> new RecentPost(p, p.getLikeCount(), p.getCommentCount()))
                .toList();
        return new Dashboard(owner, days,
                days.stream().mapToLong(Day::blogVisits).max().orElse(0),
                days.stream().mapToLong(Day::blogVisits).sum(),
                days.stream().mapToLong(Day::postViews).sum(),
                subscriptionRepository.countByBlogOwner(owner),
                postRepository.countByAuthorUsernameAndStatus(username, PostStatus.PUBLIC),
                postRepository.countLikesReceived(username),
                postRepository.countCommentsReceived(username),
                recent);
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}
