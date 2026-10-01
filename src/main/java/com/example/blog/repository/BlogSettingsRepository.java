package com.example.blog.repository;

import com.example.blog.domain.BlogSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlogSettingsRepository extends JpaRepository<BlogSettings, Long> {
}
