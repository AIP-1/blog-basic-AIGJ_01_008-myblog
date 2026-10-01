package com.example.blog.web;

import com.example.blog.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/** 알림 목록, 알림 열기(읽음 처리 후 해당 댓글로 이동), 모두 읽음 */
@Controller
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page, Authentication auth, Model model) {
        model.addAttribute("notifications", notificationService.list(auth.getName(), page));
        return "notifications";
    }

    @GetMapping("/{id}")
    public String open(@PathVariable Long id, Authentication auth) {
        return "redirect:" + notificationService.open(id, auth.getName());
    }

    @PostMapping("/read-all")
    public String readAll(Authentication auth) {
        notificationService.markAllRead(auth.getName());
        return "redirect:/notifications";
    }
}
