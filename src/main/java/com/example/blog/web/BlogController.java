package com.example.blog.web;

import com.example.blog.domain.User;
import com.example.blog.service.BlogService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** 회원 개인 블로그(/blog/{username}), 회원 블로그 목록(/blogs), 구독 피드(/feed) */
@Controller
public class BlogController {

    private final BlogService blogService;
    private final BlogVisits blogVisits;

    public BlogController(BlogService blogService, BlogVisits blogVisits) {
        this.blogService = blogService;
        this.blogVisits = blogVisits;
    }

    @GetMapping("/blogs")
    public String blogs(Model model) {
        model.addAttribute("blogs", blogService.blogs());
        return "blogs/list";
    }

    @GetMapping("/blog/{username}")
    public String home(@PathVariable String username,
                       @RequestParam(required = false) Long category,
                       @RequestParam(required = false, defaultValue = "") String q,
                       @RequestParam(defaultValue = "0") int page,
                       Authentication auth, HttpSession session, Model model) {
        // 방문을 먼저 센 뒤에 불러와야 화면의 방문 수에 이번 방문이 반영된다
        blogVisits.record(username, auth, session);
        User owner = blogService.owner(username);
        String me = auth == null ? null : auth.getName();
        model.addAttribute("owner", owner);
        model.addAttribute("blogCategories", blogService.categories(username));
        model.addAttribute("category", category);
        model.addAttribute("posts", blogService.posts(username, category, q, page));
        model.addAttribute("q", q);
        model.addAttribute("postCount", blogService.publicPostCount(username));
        model.addAttribute("subscriberCount", blogService.subscriberCount(owner));
        model.addAttribute("subscribed", blogService.isSubscribed(me, username));
        model.addAttribute("isOwner", username.equals(me));
        return "blogs/home";
    }

    @PostMapping("/blog/{username}/subscribe")
    public String subscribe(@PathVariable String username, Authentication auth, RedirectAttributes redirect) {
        try {
            blogService.subscribe(auth.getName(), username);
            redirect.addFlashAttribute("message", "구독했습니다. 새 글은 구독 피드에서 볼 수 있어요.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/blog/" + username;
    }

    @PostMapping("/blog/{username}/unsubscribe")
    public String unsubscribe(@PathVariable String username, Authentication auth, RedirectAttributes redirect) {
        blogService.unsubscribe(auth.getName(), username);
        redirect.addFlashAttribute("message", "구독을 취소했습니다.");
        return "redirect:/blog/" + username;
    }

    @GetMapping("/feed")
    public String feed(@RequestParam(defaultValue = "0") int page, Authentication auth, Model model) {
        model.addAttribute("posts", blogService.feed(auth.getName(), page));
        model.addAttribute("subscriptions", blogService.subscriptions(auth.getName()));
        return "blogs/feed";
    }
}
