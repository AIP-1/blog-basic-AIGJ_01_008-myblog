package com.example.blog.repository;

import com.example.blog.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    /** 공용 카테고리 */
    List<Category> findByOwnerIsNullOrderBySortOrderAscIdAsc();

    Optional<Category> findByOwnerIsNullAndName(String name);

    long countByOwnerIsNull();

    /** 개인 블로그 카테고리 */
    List<Category> findByOwnerUsernameOrderBySortOrderAscIdAsc(String username);

    Optional<Category> findByOwnerUsernameAndName(String username, String name);
}
