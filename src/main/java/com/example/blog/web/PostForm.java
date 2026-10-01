package com.example.blog.web;

import com.example.blog.domain.PostStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class PostForm {

    @NotBlank(message = "제목을 입력하세요.")
    @Size(max = 200, message = "제목은 200자 이하로 입력하세요.")
    private String title;

    /** null 이면 미분류 */
    private Long categoryId;

    @NotBlank(message = "내용을 입력하세요.")
    @Size(max = 100000, message = "내용이 너무 깁니다.")
    private String content;

    /** 발행할 때 공개/비공개 */
    @NotNull(message = "공개 설정을 선택하세요.")
    private PostStatus status = PostStatus.PUBLIC;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public PostStatus getStatus() {
        return status;
    }

    public void setStatus(PostStatus status) {
        this.status = status;
    }

    /** 발행 버튼으로는 공개·비공개만 고를 수 있다 */
    public PostStatus publishStatus() {
        return status == PostStatus.PRIVATE ? PostStatus.PRIVATE : PostStatus.PUBLIC;
    }
}
