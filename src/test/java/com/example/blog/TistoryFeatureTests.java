package com.example.blog;

import com.example.blog.domain.Post;
import com.example.blog.domain.PostStatus;
import com.example.blog.repository.CategoryRepository;
import com.example.blog.repository.PostRepository;
import com.example.blog.service.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "blog.upload-dir=${java.io.tmpdir}/java-blog-test-uploads"})
@AutoConfigureMockMvc
class TistoryFeatureTests {

    /** 1x1 투명 PNG */
    private static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=");

    @Autowired MockMvc mvc;
    @Autowired UserService userService;
    @Autowired PostRepository postRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired ObjectMapper objectMapper;

    @BeforeEach
    void users() {
        for (String name : new String[]{"writer", "other"}) {
            if (!userService.exists(name)) {
                userService.register(name, "password123", "USER");
            }
        }
    }

    private long saveDraft(Long id, String title, String content, String username) throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "title", title, "content", content, "id", id == null ? "" : id));
        String json = mvc.perform(post("/posts/draft").with(csrf()).with(user(username))
                        .contentType("application/json").content(body.replace("\"id\":\"\"", "\"id\":null")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(json);
        assertThat(node.get("savedAt").asText()).matches("\\d{2}:\\d{2}:\\d{2}");
        return node.get("id").asLong();
    }

    @Test
    void 임시저장_글은_목록에_안_보이고_발행하면_공개된다() throws Exception {
        long id = saveDraft(null, "임시저장 테스트 글", "작성 중인 **내용**", "writer");
        assertThat(postRepository.findById(id)).get().extracting(Post::getStatus).isEqualTo(PostStatus.DRAFT);

        // 같은 id 로 다시 저장하면 새 글이 아니라 같은 글이 바뀐다
        assertThat(saveDraft(id, "임시저장 테스트 글 v2", "더 쓴 내용", "writer")).isEqualTo(id);

        mvc.perform(get("/").param("q", "임시저장 테스트"))
                .andExpect(content().string(not(containsString("임시저장 테스트 글 v2"))));
        mvc.perform(get("/posts/" + id)).andExpect(status().isNotFound());
        mvc.perform(get("/posts/new").with(user("writer")))
                .andExpect(content().string(containsString("임시저장 테스트 글 v2")));

        mvc.perform(post("/posts/" + id + "/edit").with(csrf()).with(user("writer"))
                        .param("title", "발행된 글").param("content", "본문").param("status", "PUBLIC"))
                .andExpect(redirectedUrl("/posts/" + id));
        mvc.perform(get("/posts/" + id)).andExpect(status().isOk());

        // 발행한 글은 임시저장으로 덮어쓸 수 없다
        mvc.perform(post("/posts/draft").with(csrf()).with(user("writer")).contentType("application/json")
                        .content("{\"id\":" + id + ",\"title\":\"x\",\"content\":\"y\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void 남의_임시저장_글에는_저장할_수_없다() throws Exception {
        long id = saveDraft(null, "writer 의 초안", "내용", "writer");
        mvc.perform(post("/posts/draft").with(csrf()).with(user("other")).contentType("application/json")
                        .content("{\"id\":" + id + ",\"title\":\"x\",\"content\":\"y\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void 비공개_글은_작성자만_볼_수_있다() throws Exception {
        String location = mvc.perform(post("/posts/new").with(csrf()).with(user("writer"))
                        .param("title", "나만 보는 비공개 글").param("content", "비밀").param("status", "PRIVATE"))
                .andReturn().getResponse().getRedirectedUrl();

        mvc.perform(get(location).with(user("writer")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("비공개 글이에요")));
        mvc.perform(get(location).with(user("other"))).andExpect(status().isNotFound());
        mvc.perform(get(location)).andExpect(status().isNotFound());
        mvc.perform(get("/").param("q", "나만 보는"))
                .andExpect(content().string(not(containsString("나만 보는 비공개 글"))));

        // 관리 페이지에서 공개로 바꾸면 누구나 볼 수 있다
        Long id = Long.valueOf(location.substring(location.lastIndexOf('/') + 1));
        mvc.perform(post("/manage/posts/" + id + "/status").with(csrf()).with(user("writer")).param("status", "PUBLIC"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get(location)).andExpect(status().isOk());
    }

    @Test
    void 관리_페이지는_내_글만_보여준다() throws Exception {
        saveDraft(null, "writer 전용 관리 글", "내용", "writer");
        mvc.perform(get("/manage/posts").with(user("other")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("writer 전용 관리 글"))));
        mvc.perform(get("/manage/posts").param("status", "DRAFT").with(user("writer")))
                .andExpect(content().string(containsString("writer 전용 관리 글")));
    }

    @Test
    void 이미지를_올리면_주소로_볼_수_있다() throws Exception {
        String json = mvc.perform(multipart("/api/uploads")
                        .file(new MockMultipartFile("image", "photo.png", "image/png", PNG))
                        .with(csrf()).with(user("writer")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String url = objectMapper.readTree(json).get("url").asText();
        assertThat(url).startsWith("/uploads/").endsWith(".png");

        mvc.perform(get(url)).andExpect(status().isOk()).andExpect(content().bytes(PNG));
    }

    @Test
    void 이미지가_아닌_파일은_거부한다() throws Exception {
        // 이름과 Content-Type 을 속여도 내용으로 판별한다
        mvc.perform(multipart("/api/uploads")
                        .file(new MockMultipartFile("image", "evil.png", "image/png", "<svg onload=alert(1)>".getBytes()))
                        .with(csrf()).with(user("writer")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
        mvc.perform(multipart("/api/uploads")
                        .file(new MockMultipartFile("image", "a.png", "image/png", PNG)).with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void 일반_사용자는_카테고리와_설정을_바꿀_수_없다() throws Exception {
        mvc.perform(get("/manage/categories").with(user("writer"))).andExpect(status().isForbidden());
        mvc.perform(post("/manage/settings").with(csrf()).with(user("writer"))
                        .param("title", "해킹").param("description", ""))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void 관리자는_카테고리를_추가_수정_삭제한다() throws Exception {
        mvc.perform(post("/manage/categories").with(csrf()).param("name", "스프링"))
                .andExpect(flash().attribute("message", "카테고리를 추가했습니다."));
        mvc.perform(post("/manage/categories").with(csrf()).param("name", "스프링"))
                .andExpect(flash().attribute("error", "이미 있는 카테고리입니다: 스프링"));

        Long id = categoryRepository.findByOwnerIsNullAndName("스프링").orElseThrow().getId();
        mvc.perform(post("/manage/categories/" + id + "/rename").with(csrf()).param("name", "Spring Boot"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/")).andExpect(content().string(containsString("Spring Boot")));

        // 글이 있는 카테고리를 지우면 글은 미분류가 된다
        String location = mvc.perform(post("/posts/new").with(csrf())
                        .param("title", "스프링 첫 글").param("content", "본문").param("categoryId", id.toString()))
                .andReturn().getResponse().getRedirectedUrl();
        mvc.perform(post("/manage/categories/" + id + "/delete").with(csrf())).andExpect(status().is3xxRedirection());
        assertThat(categoryRepository.findById(id)).isEmpty();
        mvc.perform(get(location)).andExpect(content().string(containsString("미분류")));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void 관리자는_카테고리_순서를_바꾼다() throws Exception {
        var before = categoryRepository.findByOwnerIsNullOrderBySortOrderAscIdAsc();
        Long second = before.get(1).getId();
        mvc.perform(post("/manage/categories/" + second + "/move").with(csrf()).param("direction", "up"))
                .andExpect(status().is3xxRedirection());
        assertThat(categoryRepository.findByOwnerIsNullOrderBySortOrderAscIdAsc().get(0).getId()).isEqualTo(second);
        // 원래대로
        mvc.perform(post("/manage/categories/" + second + "/move").with(csrf()).param("direction", "down"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void 블로그_이름을_바꾸면_화면에_반영된다() throws Exception {
        mvc.perform(post("/manage/settings").with(csrf())
                        .param("title", "민수의 자바 노트").param("description", "자바를 공부하는 블로그"))
                .andExpect(flash().attribute("message", "저장했습니다."));
        mvc.perform(get("/"))
                .andExpect(content().string(containsString("☕ 민수의 자바 노트")))
                .andExpect(content().string(containsString("자바를 공부하는 블로그")));
        // 다른 테스트에 영향이 없도록 원래대로
        mvc.perform(post("/manage/settings").with(csrf())
                .param("title", "Java 블로그").param("description", "Java 입문·중급 강좌와 브라우저에서 바로 돌려보는 코드 실행기가 있는 블로그입니다."));
    }
}
