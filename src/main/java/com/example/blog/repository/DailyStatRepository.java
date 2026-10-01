package com.example.blog.repository;

import com.example.blog.domain.DailyStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface DailyStatRepository extends JpaRepository<DailyStat, Long> {

    /** 그날 행이 있으면 +1 하고 1, 없으면 0 을 돌려준다 (없으면 새로 만든다) */
    @Modifying
    @Query("update DailyStat s set s.blogVisits = s.blogVisits + 1 where s.owner.username = :username and s.day = :day")
    int increaseBlogVisits(@Param("username") String username, @Param("day") LocalDate day);

    @Modifying
    @Query("update DailyStat s set s.postViews = s.postViews + 1 where s.owner.username = :username and s.day = :day")
    int increasePostViews(@Param("username") String username, @Param("day") LocalDate day);

    List<DailyStat> findByOwnerUsernameAndDayBetween(String username, LocalDate from, LocalDate to);
}
