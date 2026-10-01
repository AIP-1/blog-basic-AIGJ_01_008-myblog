package com.example.blog.web;

import com.example.blog.domain.Category;
import com.example.blog.domain.Post;
import com.example.blog.domain.PostStatus;
import com.example.blog.repository.BlogSummary;
import com.example.blog.service.BlockService;
import com.example.blog.service.BlogService;
import com.example.blog.service.CategoryService;
import com.example.blog.service.MarkdownService;
import com.example.blog.service.PostService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Controller
public class PostController {

    private final PostService postService;
    private final CategoryService categoryService;
    private final MarkdownService markdownService;
    private final BlogService blogService;
    private final BlogVisits blogVisits;
    private final PostViews postViews;
    private final BlockService blockService;

    public PostController(PostService postService, CategoryService categoryService, MarkdownService markdownService,
                          BlogService blogService, BlogVisits blogVisits, PostViews postViews,
                          BlockService blockService) {
        this.postService = postService;
        this.categoryService = categoryService;
        this.markdownService = markdownService;
        this.blogService = blogService;
        this.blogVisits = blogVisits;
        this.postViews = postViews;
        this.blockService = blockService;
    }

    private static final int SIDEBAR_BLOGS = 5;

    @ModelAttribute("categories")
    public List<Category> categories() {
        return categoryService.list();
    }

    /** 글쓰기 화면의 '내 블로그' 카테고리 */
    @ModelAttribute("myCategories")
    public List<Category> myCategories(Authentication auth) {
        return auth == null ? List.of() : categoryService.listOf(auth.getName());
    }

    /** 사이드바의 '내 블로그' 카드 (로그인했을 때만) */
    @ModelAttribute("myBlog")
    public BlogService.MyBlog myBlog(Authentication auth) {
        return auth == null ? null : blogService.myBlog(auth.getName()).orElse(null);
    }

    /** 사이드바의 공지사항 */
    @ModelAttribute("notices")
    public List<Post> notices() {
        return postService.notices();
    }

    /** 사이드바의 인기 글 (조회수 상위) */
    @ModelAttribute("popularPosts")
    public List<Post> popularPosts(Authentication auth) {
        return postService.popular(auth == null ? null : auth.getName());
    }

    /** 사이드바의 회원 블로그 (방문 순위 상위) */
    @ModelAttribute("memberBlogs")
    public List<BlogSummary> memberBlogs() {
        List<BlogSummary> blogs = blogService.blogs();
        return blogs.subList(0, Math.min(SIDEBAR_BLOGS, blogs.size()));
    }

    @GetMapping("/")
    public String list(@RequestParam(required = false) Long category,
                       @RequestParam(required = false, defaultValue = "") String q,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                       @RequestParam(defaultValue = "0") int page,
                       Authentication auth, Model model) {
        model.addAttribute("posts", postService.search(category, q, date, page, auth == null ? null : auth.getName()));
        model.addAttribute("date", date);
        model.addAttribute("curriculum", postService.curriculum());
        model.addAttribute("category", category);
        model.addAttribute("q", q);
        return "posts/list";
    }

    /** 사이드바 달력: month=2026-10 → {"year":2026,"month":10,"days":{"1":2,"15":1}} */
    @GetMapping("/calendar")
    @ResponseBody
    public Map<String, Object> calendar(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        YearMonth m = month == null ? YearMonth.now() : month;
        return Map.of("year", m.getYear(), "month", m.getMonthValue(), "days", postService.postCountsByDay(m));
    }

    @GetMapping("/posts/{id}")
    public String detail(@PathVariable Long id, Authentication auth, HttpSession session, Model model) {
        Post post = postService.getVisible(id, auth);
        if (post.isPublic()) {
            blogVisits.record(post.getAuthor().getUsername(), auth, session);
            postViews.record(id, auth, session);
        }
        model.addAttribute("post", post);
        model.addAttribute("html", markdownService.toHtml(post.getContent()));
        model.addAttribute("summary", markdownService.summary(post.getContent(), 150));
        model.addAttribute("canEdit", postService.canEdit(post, auth));
        model.addAttribute("like", postService.likeState(post, auth));
        String me = auth == null ? null : auth.getName();
        model.addAttribute("blockedIds", blockService.blockedIds(me));
        model.addAttribute("authorBlocked", blockService.isBlocked(me, post.getAuthor().getUsername()));
        model.addAttribute("prev", postService.previous(post).orElse(null));
        model.addAttribute("next", postService.next(post).orElse(null));
        model.addAttribute("curriculum", postService.curriculum());
        return "posts/detail";
    }

    @GetMapping("/posts/new")
    public String newForm(@ModelAttribute("form") PostForm form, Authentication auth, Model model) {
        model.addAttribute("drafts", postService.drafts(auth.getName()));
        model.addAttribute("isDraft", true);
        return "posts/form";
    }

    @PostMapping("/posts/new")
    public String create(@Valid @ModelAttribute("form") PostForm form, BindingResult result,
                         Authentication auth, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("drafts", postService.drafts(auth.getName()));
            model.addAttribute("isDraft", true);
            return "posts/form";
        }
        Post post = postService.create(form.getTitle(), form.getContent(), form.getCategoryId(),
                form.publishStatus(), auth.getName());
        return "redirect:/posts/" + post.getId();
    }

    @GetMapping("/posts/{id}/edit")
    public String editForm(@PathVariable Long id, Authentication auth, Model model) {
        Post post = postService.get(id);
        if (!postService.canEdit(post, auth)) {
            throw new AccessDeniedException("권한이 없습니다.");
        }
        PostForm form = new PostForm();
        form.setTitle(post.getTitle());
        form.setCategoryId(post.getCategory() == null ? null : post.getCategory().getId());
        form.setContent(post.getContent());
        // 임시저장 글은 발행할 때 기본값을 '공개'로
        form.setStatus(post.isDraft() ? PostStatus.PUBLIC : post.getStatus());
        model.addAttribute("form", form);
        // 관리자가 남의 글을 고칠 때도 글쓴이의 카테고리를 고를 수 있도록
        model.addAttribute("myCategories", categoryService.listOf(post.getAuthor().getUsername()));
        model.addAttribute("postId", id);
        model.addAttribute("isDraft", post.isDraft());
        model.addAttribute("savedAt", post.isDraft() ? post.getUpdatedAt() : null);
        return "posts/form";
    }

    @PostMapping("/posts/{id}/edit")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") PostForm form,
                         BindingResult result, Authentication auth, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("postId", id);
            model.addAttribute("isDraft", postService.get(id).isDraft());
            return "posts/form";
        }
        postService.update(id, form.getTitle(), form.getContent(), form.getCategoryId(), form.publishStatus(), auth);
        return "redirect:/posts/" + id;
    }

    public record DraftRequest(Long id, String title, Long categoryId, String content) {
    }

    /** 에디터의 자동/수동 임시저장 */
    @PostMapping("/posts/draft")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveDraft(@RequestBody DraftRequest request, Authentication auth) {
        if (request.content() != null && request.content().length() > 100000) {
            return ResponseEntity.badRequest().body(Map.of("error", "내용이 너무 깁니다."));
        }
        try {
            Post post = postService.saveDraft(request.id(), request.title(), request.content(), request.categoryId(), auth);
            return ResponseEntity.ok(Map.of(
                    "id", post.getId(),
                    "savedAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/posts/{id}/delete")
    public String delete(@PathVariable Long id, Authentication auth) {
        postService.delete(id, auth);
        return "redirect:/";
    }

    /** 좋아요 토글. 화면에서는 fetch 로 부르고(JSON), 스크립트가 없으면 폼 전송 후 글로 돌아간다 */
    @PostMapping("/posts/{id}/like")
    public ResponseEntity<?> toggleLike(@PathVariable Long id, Authentication auth,
                                        @RequestHeader(value = "Accept", defaultValue = "") String accept) {
        PostService.LikeState state = postService.toggleLike(id, auth);
        if (accept.contains("application/json")) {
            return ResponseEntity.ok(Map.of("liked", state.liked(), "count", state.count()));
        }
        return ResponseEntity.status(HttpStatus.SEE_OTHER)
                .location(URI.create("/posts/" + id + "#like")).build();
    }

    @PostMapping("/posts/{id}/comments")
    public String addComment(@PathVariable Long id, @RequestParam String content,
                             @RequestParam(required = false) Long parentId, Authentication auth,
                             RedirectAttributes redirect) {
        if (StringUtils.hasText(content)) {
            try {
                postService.addComment(id, content.trim(), parentId, auth);
            } catch (IllegalStateException e) {
                redirect.addFlashAttribute("commentError", e.getMessage());
                return "redirect:/posts/" + id + "#comments";
            }
        }
        return "redirect:/posts/" + id + (parentId == null ? "#comments" : "#comment-" + parentId);
    }

    @PostMapping("/comments/{id}/delete")
    public String deleteComment(@PathVariable Long id, Authentication auth) {
        Long postId = postService.deleteComment(id, auth);
        return "redirect:/posts/" + postId + "#comments";
    }
}
