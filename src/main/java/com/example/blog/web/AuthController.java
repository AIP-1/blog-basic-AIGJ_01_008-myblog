package com.example.blog.web;

import com.example.blog.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginForm() {
        return "auth/login";
    }

    @GetMapping("/signup")
    public String signupForm(@ModelAttribute("form") SignupForm form) {
        return "auth/signup";
    }

    @PostMapping("/signup")
    public String signup(@Valid @ModelAttribute("form") SignupForm form, BindingResult result,
                         RedirectAttributes redirect) {
        if (!result.hasFieldErrors("password") && !form.getPassword().equals(form.getPasswordConfirm())) {
            result.rejectValue("passwordConfirm", "mismatch", "비밀번호가 일치하지 않습니다.");
        }
        if (!result.hasFieldErrors("username") && userService.exists(form.getUsername())) {
            result.rejectValue("username", "duplicate", "이미 사용 중인 아이디입니다.");
        }
        if (result.hasErrors()) {
            return "auth/signup";
        }
        userService.register(form.getUsername(), form.getPassword(), "USER");
        redirect.addFlashAttribute("message", "회원가입이 완료되었습니다. 로그인해 주세요.");
        return "redirect:/login";
    }
}
