package com.example.blog.web;

import com.example.blog.domain.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class PostForm {

    @NotBlank(message = "제목을 입력하세요.")
    @Size(max = 200, message = "제목은 200자 이하로 입력하세요.")
    private String title;

    @NotNull(message = "카테고리를 선택하세요.")
    private Category category = Category.FREE;

    @NotBlank(message = "내용을 입력하세요.")
    @Size(max = 100000, message = "내용이 너무 깁니다.")
    private String content;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
