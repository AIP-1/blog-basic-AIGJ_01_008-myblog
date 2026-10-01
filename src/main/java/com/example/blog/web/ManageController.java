package com.example.blog.web;

import com.example.blog.domain.Category;
import com.example.blog.domain.PostStatus;
import com.example.blog.service.BlogSettingsService;
import com.example.blog.service.CategoryService;
import com.example.blog.service.PostService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 티스토리 관리 화면처럼: 글 관리(모든 사용자), 카테고리·블로그 설정(관리자) */
@Controller
@RequestMapping("/manage")
public class ManageController {

    private static final int MAX_TITLE = 50;
    private static final int MAX_DESCRIPTION = 300;

    private final PostService postService;
    private final CategoryService categoryService;
    private final BlogSettingsService blogSettingsService;

    public ManageController(PostService postService, CategoryService categoryService,
                            BlogSettingsService blogSettingsService) {
        this.postService = postService;
        this.categoryService = categoryService;
        this.blogSettingsService = blogSettingsService;
    }

    @ModelAttribute("isAdmin")
    public boolean isAdmin(Authentication auth) {
        return postService.isAdmin(auth);
    }

    // ===== 글 관리 =====

    @GetMapping
    public String posts(@RequestParam(required = false) PostStatus status,
                        @RequestParam(defaultValue = "0") int page,
                        Authentication auth, Model model) {
        model.addAttribute("menu", "posts");
        model.addAttribute("status", status);
        model.addAttribute("statuses", PostStatus.values());
        model.addAttribute("posts", postService.manageList(status, page, auth));
        Map<String, Long> counts = postService.manageCounts(auth);
        model.addAttribute("counts", counts);
        model.addAttribute("total", counts.values().stream().mapToLong(Long::longValue).sum());
        return "manage/posts";
    }

    @PostMapping("/posts/{id}/status")
    public String changeStatus(@PathVariable Long id, @RequestParam PostStatus status,
                               @RequestParam(required = false) PostStatus filter,
                               Authentication auth, RedirectAttributes redirect) {
        postService.changeStatus(id, status, auth);
        redirect.addFlashAttribute("message", "'" + status.getLabel() + "'(으)로 바꿨습니다.");
        return filter == null ? "redirect:/manage" : "redirect:/manage?status=" + filter;
    }

    @PostMapping("/posts/{id}/delete")
    public String deletePost(@PathVariable Long id, @RequestParam(required = false) PostStatus filter,
                             Authentication auth, RedirectAttributes redirect) {
        postService.delete(id, auth);
        redirect.addFlashAttribute("message", "글을 삭제했습니다.");
        return filter == null ? "redirect:/manage" : "redirect:/manage?status=" + filter;
    }

    // ===== 카테고리 관리 (관리자) =====

    @GetMapping("/categories")
    public String categories(Model model) {
        List<Category> categories = categoryService.list();
        Map<Long, Long> postCounts = new LinkedHashMap<>();
        categories.forEach(c -> postCounts.put(c.getId(), categoryService.postCount(c)));
        model.addAttribute("menu", "categories");
        model.addAttribute("categories", categories);
        model.addAttribute("postCounts", postCounts);
        return "manage/categories";
    }

    @PostMapping("/categories")
    public String addCategory(@RequestParam String name, RedirectAttributes redirect) {
        return handle(redirect, () -> categoryService.create(name), "카테고리를 추가했습니다.");
    }

    @PostMapping("/categories/{id}/rename")
    public String renameCategory(@PathVariable Long id, @RequestParam String name, RedirectAttributes redirect) {
        return handle(redirect, () -> categoryService.rename(id, name), "이름을 바꿨습니다.");
    }

    @PostMapping("/categories/{id}/move")
    public String moveCategory(@PathVariable Long id, @RequestParam String direction, RedirectAttributes redirect) {
        return handle(redirect, () -> categoryService.move(id, "up".equals(direction)), null);
    }

    @PostMapping("/categories/{id}/delete")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirect) {
        return handle(redirect, () -> categoryService.delete(id), "카테고리를 삭제했습니다. 속해 있던 글은 미분류가 됩니다.");
    }

    private String handle(RedirectAttributes redirect, Runnable action, String successMessage) {
        try {
            action.run();
            if (successMessage != null) {
                redirect.addFlashAttribute("message", successMessage);
            }
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/manage/categories";
    }

    // ===== 블로그 설정 (관리자) =====

    @GetMapping("/settings")
    public String settings(Model model) {
        model.addAttribute("menu", "settings");
        return "manage/settings";
    }

    @PostMapping("/settings")
    public String saveSettings(@RequestParam String title, @RequestParam String description,
                               RedirectAttributes redirect) {
        if (title.isBlank() || title.trim().length() > MAX_TITLE) {
            redirect.addFlashAttribute("error", "블로그 이름은 1~" + MAX_TITLE + "자로 입력하세요.");
        } else if (description.trim().length() > MAX_DESCRIPTION) {
            redirect.addFlashAttribute("error", "소개는 " + MAX_DESCRIPTION + "자 이하로 입력하세요.");
        } else {
            blogSettingsService.update(title, description);
            redirect.addFlashAttribute("message", "저장했습니다.");
        }
        return "redirect:/manage/settings";
    }
}
