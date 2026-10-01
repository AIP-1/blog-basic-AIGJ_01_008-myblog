package com.example.blog.web;

import com.example.blog.domain.Category;
import com.example.blog.domain.Post;
import com.example.blog.service.MarkdownService;
import com.example.blog.service.PostService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
public class PostController {

    private final PostService postService;
    private final MarkdownService markdownService;

    public PostController(PostService postService, MarkdownService markdownService) {
        this.postService = postService;
        this.markdownService = markdownService;
    }

    @ModelAttribute("categories")
    public Category[] categories() {
        return Category.values();
    }

    @GetMapping("/")
    public String list(@RequestParam(required = false) Category category,
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
        Post post = postService.get(id);
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
    public String newForm(@ModelAttribute("form") PostForm form) {
        return "posts/form";
    }

    @PostMapping("/posts/new")
    public String create(@Valid @ModelAttribute("form") PostForm form, BindingResult result, Authentication auth) {
        if (result.hasErrors()) {
            return "posts/form";
        }
        Post post = postService.create(form.getTitle(), form.getContent(), form.getCategory(), auth.getName());
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
        form.setCategory(post.getCategory());
        form.setContent(post.getContent());
        model.addAttribute("form", form);
        model.addAttribute("postId", id);
        return "posts/form";
    }

    @PostMapping("/posts/{id}/edit")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") PostForm form,
                         BindingResult result, Authentication auth, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("postId", id);
            return "posts/form";
        }
        postService.update(id, form.getTitle(), form.getContent(), form.getCategory(), auth);
        return "redirect:/posts/" + id;
    }

    @PostMapping("/posts/{id}/delete")
    public String delete(@PathVariable Long id, Authentication auth) {
        postService.delete(id, auth);
        return "redirect:/";
    }

    @PostMapping("/posts/{id}/comments")
    public String addComment(@PathVariable Long id, @RequestParam String content, Authentication auth) {
        if (StringUtils.hasText(content)) {
            postService.addComment(id, content.trim(), auth.getName());
        }
        return "redirect:/posts/" + id + "#comments";
    }

    @PostMapping("/comments/{id}/delete")
    public String deleteComment(@PathVariable Long id, Authentication auth) {
        Long postId = postService.deleteComment(id, auth);
        return "redirect:/posts/" + postId + "#comments";
    }
}
