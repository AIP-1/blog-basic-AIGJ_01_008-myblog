package com.example.blog.web;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class TimeAgoTests {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 1, 15, 0);
    private final TimeAgo timeAgo = new TimeAgo(
            Clock.fixed(NOW.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault()));

    @Test
    void 상대_시간으로_보여준다() {
        assertThat(timeAgo.format(NOW.minusSeconds(30))).isEqualTo("방금 전");
        assertThat(timeAgo.format(NOW.plusMinutes(2))).isEqualTo("방금 전");
        assertThat(timeAgo.format(NOW.minusMinutes(5))).isEqualTo("5분 전");
        assertThat(timeAgo.format(NOW.minusMinutes(59))).isEqualTo("59분 전");
        assertThat(timeAgo.format(NOW.minusHours(3))).isEqualTo("3시간 전");
        assertThat(timeAgo.format(NOW.minusHours(23))).isEqualTo("23시간 전");
        assertThat(timeAgo.format(NOW.minusDays(1))).isEqualTo("1일 전");
        assertThat(timeAgo.format(NOW.minusDays(29))).isEqualTo("29일 전");
        assertThat(timeAgo.format(NOW.minusMonths(3))).isEqualTo("3개월 전");
        assertThat(timeAgo.format(NOW.minusMonths(11))).isEqualTo("11개월 전");
        assertThat(timeAgo.format(NOW.minusYears(2))).isEqualTo("2년 전");
        assertThat(timeAgo.format(null)).isEmpty();
    }

    @Test
    void 정확한_시각은_툴팁용으로() {
        assertThat(timeAgo.exact(NOW)).isEqualTo("2026-10-01 15:00");
    }
}
