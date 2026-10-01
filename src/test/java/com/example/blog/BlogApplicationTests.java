package com.example.blog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "blog.upload-dir=${java.io.tmpdir}/java-blog-test-uploads"})
@AutoConfigureMockMvc
class BlogApplicationTests {

    @Autowired
    MockMvc mvc;

    @Test
    void 홈에_강좌_목차가_보인다() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Java 설치와 Hello World")))
                .andExpect(content().string(containsString("람다와 스트림")));
    }

    @Test
    void 강좌_글이_마크다운으로_렌더링된다() throws Exception {
        mvc.perform(get("/posts/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<code class=\"language-java\">")));
    }

    @Test
    void 글_상세에_공유_버튼과_미리보기_태그가_있다() throws Exception {
        mvc.perform(get("/posts/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<meta property=\"og:title\" content=\"1. Java 설치와 Hello World\">")))
                .andExpect(content().string(containsString("<meta property=\"og:url\" content=\"http://localhost/posts/1\">")))
                .andExpect(content().string(containsString("<meta property=\"og:image\" content=\"http://localhost/img/og-default.png\">")))
                // 요약에는 코드와 마크다운 기호가 없어야 한다
                .andExpect(content().string(containsString("<meta property=\"og:description\" content=\"Java는 한 번 작성하면")))
                .andExpect(content().string(containsString("https://twitter.com/intent/tweet?text=")))
                .andExpect(content().string(containsString("&amp;url=http://localhost/posts/1")))
                .andExpect(content().string(containsString("https://www.facebook.com/sharer/sharer.php?u=http://localhost/posts/1")));
    }

    @Test
    void 미리보기_이미지는_로그인_없이_열린다() throws Exception {
        mvc.perform(get("/img/og-default.png")).andExpect(status().isOk());
    }

    @Test
    void 비로그인_사용자는_글쓰기시_로그인으로_이동한다() throws Exception {
        mvc.perform(get("/posts/new"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void 로그인_사용자는_글을_쓸_수_있다() throws Exception {
        mvc.perform(post("/posts/new").with(csrf())
                        .param("title", "테스트 글")
                        .param("content", "<script>alert(1)</script> **굵게**"))
                .andExpect(status().is3xxRedirection());

        mvc.perform(get("/").param("q", "테스트 글"))
                .andExpect(content().string(containsString("테스트 글")));
    }

    @Test
    void 비로그인_사용자는_코드를_실행할_수_없다() throws Exception {
        mvc.perform(post("/playground/run").with(csrf())
                        .contentType("application/json")
                        .content("{\"code\":\"System.out.println(1);\"}"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "admin")
    void 로그인_사용자는_코드를_실행할_수_있다() throws Exception {
        mvc.perform(get("/playground")).andExpect(status().isOk());
        mvc.perform(post("/playground/run").with(csrf())
                        .contentType("application/json")
                        .content("{\"code\":\"System.out.println(1 + 1);\",\"input\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.output").value("2\n"));
    }

    @Test
    @WithMockUser(username = "someone")
    void 다른_사람의_강좌_글은_삭제할_수_없다() throws Exception {
        mvc.perform(post("/posts/1/delete").with(csrf()))
                .andExpect(status().isForbidden());
    }
}
