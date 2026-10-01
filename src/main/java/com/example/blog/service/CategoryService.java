package com.example.blog.service;

import com.example.blog.domain.Category;
import com.example.blog.domain.User;
import com.example.blog.repository.CategoryRepository;
import com.example.blog.repository.PostRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 카테고리는 두 종류: 공용(owner = null, 관리자가 관리)과 회원 개인 블로그용(owner = 그 회원).
 * 아래 메서드의 owner 인자가 null 이면 공용 카테고리를 다룬다.
 */
@Service
@Transactional(readOnly = true)
public class CategoryService {

    public static final int MAX_NAME_LENGTH = 30;

    private final CategoryRepository categoryRepository;
    private final PostRepository postRepository;

    public CategoryService(CategoryRepository categoryRepository, PostRepository postRepository) {
        this.categoryRepository = categoryRepository;
        this.postRepository = postRepository;
    }

    /** 공용 카테고리 */
    public List<Category> list() {
        return categoryRepository.findByOwnerIsNullOrderBySortOrderAscIdAsc();
    }

    /** 회원 개인 블로그 카테고리 */
    public List<Category> listOf(String username) {
        return categoryRepository.findByOwnerUsernameOrderBySortOrderAscIdAsc(username);
    }

    public List<Category> list(User owner) {
        return owner == null ? list() : listOf(owner.getUsername());
    }

    /** 글에 붙일 카테고리. null 이면 미분류. 공용이거나 글쓴이 본인의 카테고리만 쓸 수 있다 */
    public Category findUsable(Long id, String username) {
        if (id == null) {
            return null;
        }
        Category category = get(id);
        if (category.getOwner() != null && !category.isOwnedBy(username)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "쓸 수 없는 카테고리입니다.");
        }
        return category;
    }

    public Category get(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다."));
    }

    public long postCount(Category category) {
        return postRepository.countByCategory(category);
    }

    @Transactional
    public Category create(String name, User owner) {
        String trimmed = validName(name, owner, null);
        int nextOrder = list(owner).stream().mapToInt(Category::getSortOrder).max().orElse(0) + 1;
        return categoryRepository.save(new Category(trimmed, nextOrder, owner));
    }

    @Transactional
    public void rename(Long id, String name, User owner) {
        Category category = getOwned(id, owner);
        category.rename(validName(name, owner, category));
    }

    /** 바로 위/아래 카테고리와 순서를 바꾼다 */
    @Transactional
    public void move(Long id, boolean up, User owner) {
        List<Category> categories = new ArrayList<>(list(owner));
        int index = categories.indexOf(getOwned(id, owner));
        int target = up ? index - 1 : index + 1;
        if (index < 0 || target < 0 || target >= categories.size()) {
            return;
        }
        Collections.swap(categories, index, target);
        for (int i = 0; i < categories.size(); i++) {
            categories.get(i).changeOrder(i + 1);
        }
    }

    /** 삭제한 카테고리의 글은 미분류가 된다 */
    @Transactional
    public void delete(Long id, User owner) {
        Category category = getOwned(id, owner);
        postRepository.clearCategory(category);
        categoryRepository.delete(category);
    }

    /** 다른 사람의 카테고리는 없는 것처럼 404 */
    private Category getOwned(Long id, User owner) {
        Category category = get(id);
        Long ownerId = category.getOwner() == null ? null : category.getOwner().getId();
        if (!Objects.equals(ownerId, owner == null ? null : owner.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다.");
        }
        return category;
    }

    private String validName(String name, User owner, Category self) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("카테고리 이름을 입력하세요.");
        }
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("카테고리 이름은 " + MAX_NAME_LENGTH + "자 이하로 입력하세요.");
        }
        boolean duplicate = (owner == null
                ? categoryRepository.findByOwnerIsNullAndName(trimmed)
                : categoryRepository.findByOwnerUsernameAndName(owner.getUsername(), trimmed))
                .filter(found -> self == null || !found.getId().equals(self.getId()))
                .isPresent();
        if (duplicate) {
            throw new IllegalArgumentException("이미 있는 카테고리입니다: " + trimmed);
        }
        return trimmed;
    }
}
