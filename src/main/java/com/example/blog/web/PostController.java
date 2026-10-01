package com.example.blog.web;

import com.example.blog.domain.Category;
import com.example.blog.domain.Post;
import com.example.blog.domain.PostStatus;
import com.example.blog.service.CategoryService;
import com.example.blog.service.MarkdownService;
import com.example.blog.service.PostService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Controller
public class PostController {

    private final PostService postService;
    private final CategoryService categoryService;
    private final MarkdownService markdownService;

    public PostController(PostService postService, CategoryService categoryService, MarkdownService markdownService) {
        this.postService = postService;
        this.categoryService = categoryService;
        this.markdownService = markdownService;
    }

    @ModelAttribute("categories")
    public List<Category> categories() {
        return categoryService.list();
    }

    @GetMapping("/")
    public String list(@RequestParam(required = false) Long category,
                       @RequestParam(required = false, defaultValue = "") String q,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        model.addAttribute("posts", postService.search(category, q, page));
        model.addAttribute("curriculum", postService.curriculum());
        model.addAttribute("category", category);
        model.addAttribute("q", q);
        return "posts/list";
    }

    @GetMapping("/posts/{id}")
    public String detail(@PathVariable Long id, Authentication auth, Model model) {
        Post post = postService.getVisible(id, auth);
        model.addAttribute("post", post);
        model.addAttribute("html", markdownService.toHtml(post.getContent()));
        model.addAttribute("summary", markdownService.summary(post.getContent(), 150));
        model.addAttribute("canEdit", postService.canEdit(post, auth));
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

    @PostMapping("/posts/{id}/comments")
    public String addComment(@PathVariable Long id, @RequestParam String content, Authentication auth) {
        if (StringUtils.hasText(content)) {
            postService.addComment(id, content.trim(), auth);
        }
        return "redirect:/posts/" + id + "#comments";
    }

    @PostMapping("/comments/{id}/delete")
    public String deleteComment(@PathVariable Long id, Authentication auth) {
        Long postId = postService.deleteComment(id, auth);
        return "redirect:/posts/" + postId + "#comments";
    }
}
