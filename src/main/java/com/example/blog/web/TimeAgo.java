package com.example.blog.web;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;

/**
 * '방금 전', '5분 전', '3시간 전', '2일 전', '3개월 전', '1년 전' 같은 상대 시간.
 * 화면에서는 ${@timeAgo.format(post.createdAt)} 로 쓰고, 정확한 시각은 ${@timeAgo.exact(...)} 를 툴팁으로 붙인다.
 */
@Component("timeAgo")
public class TimeAgo {

    private static final DateTimeFormatter EXACT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final Clock clock;

    public TimeAgo() {
        this(Clock.systemDefaultZone());
    }

    TimeAgo(Clock clock) {
        this.clock = clock;
    }

    public String format(LocalDateTime time) {
        if (time == null) {
            return "";
        }
        LocalDateTime now = LocalDateTime.now(clock);
        Duration elapsed = Duration.between(time, now);
        if (elapsed.toMinutes() < 1) {
            return "방금 전"; // 1분 미만, 또는 시계 차이로 미래인 경우
        }
        if (elapsed.toHours() < 1) {
            return elapsed.toMinutes() + "분 전";
        }
        if (elapsed.toDays() < 1) {
            return elapsed.toHours() + "시간 전";
        }
        Period period = Period.between(time.toLocalDate(), now.toLocalDate());
        if (period.toTotalMonths() < 1) {
            return Math.max(1, elapsed.toDays()) + "일 전";
        }
        if (period.getYears() < 1) {
            return period.toTotalMonths() + "개월 전";
        }
        return period.getYears() + "년 전";
    }

    public String exact(LocalDateTime time) {
        return time == null ? "" : time.format(EXACT);
    }
}
