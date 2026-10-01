package com.example.blog.web;

import com.example.blog.domain.Category;
import com.example.blog.domain.PostStatus;
import com.example.blog.domain.User;
import com.example.blog.service.BlockService;
import com.example.blog.service.BlogService;
import com.example.blog.service.BlogSettingsService;
import com.example.blog.service.CategoryService;
import com.example.blog.service.PostService;
import com.example.blog.service.StatsService;
import com.example.blog.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 티스토리 관리 화면처럼: 통계·글·내 블로그·구독·차단 관리(모든 사용자), 공용 카테고리·사이트 설정(관리자) */
@Controller
@RequestMapping("/manage")
public class ManageController {

    private static final int MAX_TITLE = 50;
    private static final int MAX_DESCRIPTION = 300;

    private final PostService postService;
    private final CategoryService categoryService;
    private final BlogSettingsService blogSettingsService;
    private final BlogService blogService;
    private final UserService userService;
    private final BlockService blockService;
    private final StatsService statsService;

    public ManageController(PostService postService, CategoryService categoryService,
                            BlogSettingsService blogSettingsService, BlogService blogService,
                            UserService userService, BlockService blockService, StatsService statsService) {
        this.postService = postService;
        this.categoryService = categoryService;
        this.blogSettingsService = blogSettingsService;
        this.blogService = blogService;
        this.userService = userService;
        this.blockService = blockService;
        this.statsService = statsService;
    }

    @ModelAttribute("isAdmin")
    public boolean isAdmin(Authentication auth) {
        return postService.isAdmin(auth);
    }

    // ===== 통계 (블로그 관리 첫 화면) =====

    @GetMapping
    public String dashboard(Authentication auth, Model model) {
        model.addAttribute("menu", "stats");
        model.addAttribute("stats", statsService.dashboard(auth.getName()));
        return "manage/dashboard";
    }

    // ===== 글 관리 =====

    @GetMapping("/posts")
    public String posts(@RequestParam(required = false) PostStatus status,
                        @RequestParam(defaultValue = "") String q,
                        @RequestParam(defaultValue = "0") int page,
                        Authentication auth, Model model) {
        model.addAttribute("menu", "posts");
        model.addAttribute("status", status);
        model.addAttribute("q", q);
        model.addAttribute("statuses", PostStatus.values());
        model.addAttribute("posts", postService.manageList(status, q, page, auth));
        Map<String, Long> counts = postService.manageCounts(q, auth);
        model.addAttribute("counts", counts);
        model.addAttribute("total", counts.values().stream().mapToLong(Long::longValue).sum());
        return "manage/posts";
    }

    @PostMapping("/posts/{id}/status")
    public String changeStatus(@PathVariable Long id, @RequestParam PostStatus status,
                               @RequestParam(required = false) PostStatus filter,
                               @RequestParam(defaultValue = "") String q,
                               Authentication auth, RedirectAttributes redirect) {
        postService.changeStatus(id, status, auth);
        redirect.addFlashAttribute("message", "'" + status.getLabel() + "'(으)로 바꿨습니다.");
        return backToPosts(filter, q, redirect);
    }

    /** 공지 등록/해제 (관리자). back 이 있으면 그 글 화면으로 돌아간다 */
    @PostMapping("/posts/{id}/notice")
    public String changeNotice(@PathVariable Long id, @RequestParam boolean notice,
                               @RequestParam(required = false) PostStatus filter,
                               @RequestParam(defaultValue = "") String q,
                               @RequestParam(defaultValue = "false") boolean back,
                               Authentication auth, RedirectAttributes redirect) {
        postService.changeNotice(id, notice, auth);
        redirect.addFlashAttribute("message", notice ? "공지사항으로 등록했습니다." : "공지사항에서 내렸습니다.");
        if (back) {
            return "redirect:/posts/" + id;
        }
        return backToPosts(filter, q, redirect);
    }

    @PostMapping("/posts/{id}/delete")
    public String deletePost(@PathVariable Long id, @RequestParam(required = false) PostStatus filter,
                             @RequestParam(defaultValue = "") String q,
                             Authentication auth, RedirectAttributes redirect) {
        postService.delete(id, auth);
        redirect.addFlashAttribute("message", "글을 삭제했습니다.");
        return backToPosts(filter, q, redirect);
    }

    /** 글 관리 목록으로 돌아가되 보고 있던 탭(상태)과 검색어를 유지한다 */
    private String backToPosts(PostStatus filter, String q, RedirectAttributes redirect) {
        if (filter != null) {
            redirect.addAttribute("status", filter);
        }
        if (!q.isBlank()) {
            redirect.addAttribute("q", q);
        }
        return "redirect:/manage/posts";
    }

    // ===== 카테고리 관리: 공용(관리자) / 내 블로그(모든 사용자) =====

    private static final String SITE_CATEGORIES = "/manage/categories";
    private static final String MY_CATEGORIES = "/manage/blog/categories";

    @GetMapping("/categories")
    public String siteCategories(Model model) {
        return categoryPage(null, SITE_CATEGORIES, "공용 카테고리 관리", model);
    }

    @GetMapping("/blog/categories")
    public String myCategories(Authentication auth, Model model) {
        return categoryPage(userService.get(auth.getName()), MY_CATEGORIES, "내 블로그 카테고리", model);
    }

    private String categoryPage(User owner, String base, String title, Model model) {
        List<Category> categories = categoryService.list(owner);
        Map<Long, Long> postCounts = new LinkedHashMap<>();
        categories.forEach(c -> postCounts.put(c.getId(), categoryService.postCount(c)));
        model.addAttribute("menu", owner == null ? "categories" : "myCategories");
        model.addAttribute("categoryBase", base);
        model.addAttribute("pageTitle", title);
        model.addAttribute("categories", categories);
        model.addAttribute("postCounts", postCounts);
        return "manage/categories";
    }

    @PostMapping({"/categories", "/blog/categories"})
    public String addCategory(@RequestParam String name, Authentication auth, HttpServletRequest request,
                              RedirectAttributes redirect) {
        User owner = categoryOwner(request, auth);
        return handle(redirect, owner, () -> categoryService.create(name, owner), "카테고리를 추가했습니다.");
    }

    @PostMapping({"/categories/{id}/rename", "/blog/categories/{id}/rename"})
    public String renameCategory(@PathVariable Long id, @RequestParam String name, Authentication auth,
                                 HttpServletRequest request, RedirectAttributes redirect) {
        User owner = categoryOwner(request, auth);
        return handle(redirect, owner, () -> categoryService.rename(id, name, owner), "이름을 바꿨습니다.");
    }

    @PostMapping({"/categories/{id}/move", "/blog/categories/{id}/move"})
    public String moveCategory(@PathVariable Long id, @RequestParam String direction, Authentication auth,
                               HttpServletRequest request, RedirectAttributes redirect) {
        User owner = categoryOwner(request, auth);
        return handle(redirect, owner, () -> categoryService.move(id, "up".equals(direction), owner), null);
    }

    @PostMapping({"/categories/{id}/delete", "/blog/categories/{id}/delete"})
    public String deleteCategory(@PathVariable Long id, Authentication auth, HttpServletRequest request,
                                 RedirectAttributes redirect) {
        User owner = categoryOwner(request, auth);
        return handle(redirect, owner, () -> categoryService.delete(id, owner), "카테고리를 삭제했습니다. 속해 있던 글은 미분류가 됩니다.");
    }

    /** /manage/blog/... 요청이면 로그인한 사용자 본인, 아니면 공용(null) */
    private User categoryOwner(HttpServletRequest request, Authentication auth) {
        return request.getRequestURI().contains(MY_CATEGORIES) ? userService.get(auth.getName()) : null;
    }

    private String handle(RedirectAttributes redirect, User owner, Runnable action, String successMessage) {
        try {
            action.run();
            if (successMessage != null) {
                redirect.addFlashAttribute("message", successMessage);
            }
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + (owner == null ? SITE_CATEGORIES : MY_CATEGORIES);
    }

    // ===== 내 블로그 설정 (모든 사용자) =====

    @GetMapping("/blog")
    public String mySettings(Authentication auth, Model model) {
        model.addAttribute("menu", "myBlog");
        model.addAttribute("me", userService.get(auth.getName()));
        return "manage/blog";
    }

    @PostMapping("/blog")
    public String saveMySettings(@RequestParam String title, @RequestParam String intro,
                                 Authentication auth, RedirectAttributes redirect) {
        try {
            blogService.updateSettings(auth.getName(), title, intro);
            redirect.addFlashAttribute("message", "저장했습니다.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/manage/blog";
    }

    // ===== 구독 관리 (모든 사용자) =====

    @GetMapping("/subscriptions")
    public String subscriptions(Authentication auth, Model model) {
        model.addAttribute("menu", "subscriptions");
        model.addAttribute("subscriptions", blogService.subscriptions(auth.getName()));
        return "manage/subscriptions";
    }

    @PostMapping("/subscriptions/{username}/delete")
    public String unsubscribe(@PathVariable String username, Authentication auth, RedirectAttributes redirect) {
        blogService.unsubscribe(auth.getName(), username);
        redirect.addFlashAttribute("message", username + " 님의 블로그 구독을 취소했습니다.");
        return "redirect:/manage/subscriptions";
    }

    // ===== 차단 관리 (모든 사용자) =====

    @GetMapping("/blocks")
    public String blocks(Authentication auth, Model model) {
        model.addAttribute("menu", "blocks");
        model.addAttribute("blocks", blockService.blocks(auth.getName()));
        return "manage/blocks";
    }

    @PostMapping("/blocks/{username}/delete")
    public String unblock(@PathVariable String username, Authentication auth, RedirectAttributes redirect) {
        blockService.unblock(auth.getName(), username);
        redirect.addFlashAttribute("message", username + " 님의 차단을 풀었습니다.");
        return "redirect:/manage/blocks";
    }

    // ===== 회원 관리 (관리자) =====

    @GetMapping("/users")
    public String users(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "0") int page,
                        Model model) {
        model.addAttribute("menu", "users");
        model.addAttribute("q", q);
        model.addAttribute("users", userService.list(q, page));
        return "manage/users";
    }

    @PostMapping("/users/{username}/ban")
    public String ban(@PathVariable String username, @RequestParam(defaultValue = "") String reason,
                      @RequestParam(defaultValue = "") String q, RedirectAttributes redirect) {
        try {
            userService.ban(username, reason);
            redirect.addFlashAttribute("message", username + " 님의 이용을 정지했습니다.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        redirect.addAttribute("q", q);
        return "redirect:/manage/users";
    }

    @PostMapping("/users/{username}/unban")
    public String unban(@PathVariable String username, @RequestParam(defaultValue = "") String q,
                        RedirectAttributes redirect) {
        userService.unban(username);
        redirect.addFlashAttribute("message", username + " 님의 정지를 풀었습니다.");
        redirect.addAttribute("q", q);
        return "redirect:/manage/users";
    }

    // ===== 사이트 설정 (관리자) =====

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
