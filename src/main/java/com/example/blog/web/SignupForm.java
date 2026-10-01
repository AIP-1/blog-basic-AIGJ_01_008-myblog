package com.example.blog.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class SignupForm {

    @NotBlank(message = "아이디를 입력하세요.")
    @Pattern(regexp = "^[a-zA-Z0-9_]{4,20}$", message = "아이디는 영문/숫자/_ 4~20자입니다.")
    private String username;

    @NotBlank(message = "비밀번호를 입력하세요.")
    @Size(min = 8, max = 64, message = "비밀번호는 8자 이상입니다.")
    private String password;

    @NotBlank(message = "비밀번호 확인을 입력하세요.")
    private String passwordConfirm;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPasswordConfirm() {
        return passwordConfirm;
    }

    public void setPasswordConfirm(String passwordConfirm) {
        this.passwordConfirm = passwordConfirm;
    }
}
