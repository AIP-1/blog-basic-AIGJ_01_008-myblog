package com.example.blog;

import com.example.blog.domain.Category;
import com.example.blog.repository.CategoryRepository;
import com.example.blog.repository.PostRepository;
import com.example.blog.repository.SubscriptionRepository;
import com.example.blog.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:memberblog",
        "blog.upload-dir=${java.io.tmpdir}/java-blog-test-uploads"})
@AutoConfigureMockMvc
class MemberBlogTests {

    @Autowired MockMvc mvc;
    @Autowired UserService userService;
    @Autowired CategoryRepository categoryRepository;
    @Autowired SubscriptionRepository subscriptionRepository;
    @Autowired PostRepository postRepository;

    @BeforeEach
    void users() {
        for (String name : new String[]{"alice", "bob"}) {
            if (!userService.exists(name)) {
                userService.register(name, "password123", "USER");
            }
        }
    }

    @Test
    void 회원은_자기_블로그_카테고리를_만들고_글을_분류한다() throws Exception {
        mvc.perform(post("/manage/blog/categories").with(csrf()).with(user("alice")).param("name", "여행"))
                .andExpect(redirectedUrl("/manage/blog/categories"))
                .andExpect(flash().attribute("message", "카테고리를 추가했습니다."));
        // 다른 회원은 같은 이름을 따로 쓸 수 있다
        mvc.perform(post("/manage/blog/categories").with(csrf()).with(user("bob")).param("name", "여행"))
                .andExpect(flash().attribute("message", "카테고리를 추가했습니다."));

        Category travel = categoryRepository.findByOwnerUsernameAndName("alice", "여행").orElseThrow();
        assertThat(categoryRepository.findByOwnerIsNullAndName("여행")).isEmpty();

        mvc.perform(post("/posts/new").with(csrf()).with(user("alice"))
                        .param("title", "제주 여행기").param("content", "바다가 예뻤다")
                        .param("categoryId", travel.getId().toString()))
                .andExpect(status().is3xxRedirection());

        mvc.perform(get("/blog/alice").param("category", travel.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("alice의 블로그")))
                .andExpect(content().string(containsString("제주 여행기")));
        // 홈 사이드바의 공용 카테고리에는 개인 카테고리가 안 보인다
        mvc.perform(get("/")).andExpect(content().string(not(containsString("category=" + travel.getId() + "\""))));
    }

    @Test
    void 남의_카테고리는_쓰거나_고칠_수_없다() throws Exception {
        mvc.perform(post("/manage/blog/categories").with(csrf()).with(user("alice")).param("name", "일기"));
        Long diary = categoryRepository.findByOwnerUsernameAndName("alice", "일기").orElseThrow().getId();

        mvc.perform(post("/posts/new").with(csrf()).with(user("bob"))
                        .param("title", "남의 카테고리").param("content", "본문").param("categoryId", diary.toString()))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/manage/blog/categories/" + diary + "/delete").with(csrf()).with(user("bob")))
                .andExpect(status().isNotFound());
        // 일반 회원은 공용 카테고리 관리 화면에 못 들어간다
        mvc.perform(post("/manage/categories").with(csrf()).with(user("bob")).param("name", "공용"))
                .andExpect(status().isForbidden());
        assertThat(categoryRepository.findById(diary)).isPresent();
    }

    @Test
    void 블로그_이름과_소개를_바꾼다() throws Exception {
        mvc.perform(post("/manage/blog").with(csrf()).with(user("bob"))
                        .param("title", "밥의 코딩 일지").param("intro", "매일 조금씩"))
                .andExpect(flash().attribute("message", "저장했습니다."));
        mvc.perform(get("/blog/bob"))
                .andExpect(content().string(containsString("밥의 코딩 일지")))
                .andExpect(content().string(containsString("매일 조금씩")));
        mvc.perform(get("/blog/nobody")).andExpect(status().isNotFound());
    }

    @Test
    void 구독하면_피드에_새_글이_모인다() throws Exception {
        mvc.perform(post("/posts/new").with(csrf()).with(user("alice"))
                .param("title", "앨리스의 구독 테스트 글").param("content", "본문"));

        mvc.perform(get("/feed").with(user("bob")))
                .andExpect(content().string(not(containsString("앨리스의 구독 테스트 글"))));

        mvc.perform(post("/blog/alice/subscribe").with(csrf()).with(user("bob")))
                .andExpect(redirectedUrl("/blog/alice"));
        // 두 번 눌러도 한 번만 구독된다
        mvc.perform(post("/blog/alice/subscribe").with(csrf()).with(user("bob")));
        assertThat(subscriptionRepository.findBySubscriberUsernameOrderByCreatedAtDesc("bob")).hasSize(1);

        mvc.perform(get("/blog/alice").with(user("bob")))
                .andExpect(content().string(containsString("구독 중")))
                .andExpect(content().string(containsString("구독자 <span>1</span>")));
        mvc.perform(get("/feed").with(user("bob")))
                .andExpect(content().string(containsString("앨리스의 구독 테스트 글")));
        mvc.perform(get("/manage/subscriptions").with(user("bob")))
                .andExpect(content().string(containsString("alice의 블로그")));

        mvc.perform(post("/manage/subscriptions/alice/delete").with(csrf()).with(user("bob")));
        assertThat(subscriptionRepository.findBySubscriberUsernameOrderByCreatedAtDesc("bob")).isEmpty();
    }

    @Test
    void 방문이_많은_블로그가_먼저_보인다() throws Exception {
        for (String name : new String[]{"carol", "dave"}) {
            if (!userService.exists(name)) {
                userService.register(name, "password123", "USER");
            }
            mvc.perform(post("/posts/new").with(csrf()).with(user(name))
                    .param("title", name + "의 순위 테스트 글").param("content", "본문"));
        }
        long before = userService.get("dave").getBlogVisits();

        // 서로 다른 방문자(세션) 3명이 dave 블로그를, 1명이 carol 블로그를 방문
        for (int i = 0; i < 3; i++) {
            mvc.perform(get("/blog/dave").session(new MockHttpSession()));
        }
        mvc.perform(get("/blog/carol").session(new MockHttpSession()));

        // 같은 세션에서 다시 보거나 글을 읽어도 한 번만 센다
        MockHttpSession same = new MockHttpSession();
        mvc.perform(get("/blog/dave").session(same));
        mvc.perform(get("/blog/dave").session(same));
        // 주인 본인의 방문은 세지 않는다
        mvc.perform(get("/blog/dave").with(user("dave")).session(new MockHttpSession()));
        assertThat(userService.get("dave").getBlogVisits()).isEqualTo(before + 4);

        String html = mvc.perform(get("/blogs")).andReturn().getResponse().getContentAsString();
        assertThat(html.indexOf("dave의 블로그")).isLessThan(html.indexOf("carol의 블로그"));
    }

    @Test
    void 글을_읽으면_작성자_블로그_방문으로_센다() throws Exception {
        String location = mvc.perform(post("/posts/new").with(csrf()).with(user("alice"))
                        .param("title", "방문 테스트 글").param("content", "본문"))
                .andReturn().getResponse().getRedirectedUrl();
        long before = userService.get("alice").getBlogVisits();
        MockHttpSession session = new MockHttpSession();
        mvc.perform(get(location).session(session));
        mvc.perform(get("/blog/alice").session(session));
        assertThat(userService.get("alice").getBlogVisits()).isEqualTo(before + 1);
    }

    @Test
    void 검색은_작성자_제목_내용_순으로_보여준다() throws Exception {
        if (!userService.exists("kiwiman")) {
            userService.register("kiwiman", "password123", "USER");
        }
        // 일부러 '내용 일치' 글을 가장 나중에(최신으로) 쓴다
        write("kiwiman", "작성자 일치 옛 글", "본문");
        write("kiwiman", "작성자 일치 새 글", "본문");
        write("alice", "KIWI 제목 일치", "본문");
        write("bob", "그냥 글", "내용에 kiwi 가 있다");

        String html = mainContent(mvc.perform(get("/").param("q", "Kiwi"))
                .andExpect(content().string(containsString("검색 결과 <span>4</span>건")))
                .andReturn().getResponse().getContentAsString());
        assertThat(html.indexOf("작성자 일치 새 글"))
                .isLessThan(html.indexOf("작성자 일치 옛 글"));
        assertThat(html.indexOf("작성자 일치 옛 글")).isLessThan(html.indexOf("KIWI 제목 일치"));
        assertThat(html.indexOf("KIWI 제목 일치")).isLessThan(html.indexOf("그냥 글"));

        // 검색하지 않으면 최신순
        String list = mainContent(mvc.perform(get("/")).andReturn().getResponse().getContentAsString());
        assertThat(list.indexOf("그냥 글")).isLessThan(list.indexOf("작성자 일치 새 글"));
    }

    @Test
    void 블로그_안에서_검색하면_그_블로그_글만_제목_내용_순으로_나온다() throws Exception {
        write("bob", "망고 제목 글", "본문");
        write("bob", "다른 글", "내용에 망고");
        write("alice", "앨리스의 망고 글", "본문");

        String html = mvc.perform(get("/blog/bob").param("q", "망고"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("검색 결과 <span>2</span>건")))
                .andExpect(content().string(not(containsString("앨리스의 망고 글"))))
                .andReturn().getResponse().getContentAsString();
        assertThat(html.indexOf("망고 제목 글")).isLessThan(html.indexOf("다른 글"));
    }

    @Test
    void 조회수가_많은_글이_인기_글_맨_위에_나온다() throws Exception {
        String hot = mvc.perform(post("/posts/new").with(csrf()).with(user("alice"))
                        .param("title", "조회수 많은 인기 글").param("content", "본문"))
                .andReturn().getResponse().getRedirectedUrl();
        long id = Long.parseLong(hot.substring(hot.lastIndexOf('/') + 1));

        for (int i = 0; i < 30; i++) {
            mvc.perform(get(hot).session(new MockHttpSession()));
        }
        // 같은 세션에서 다시 보거나, 작성자가 보면 세지 않는다
        MockHttpSession same = new MockHttpSession();
        mvc.perform(get(hot).session(same));
        mvc.perform(get(hot).session(same));
        mvc.perform(get(hot).with(user("alice")).session(new MockHttpSession()));
        assertThat(postRepository.findById(id).orElseThrow().getViewCount()).isEqualTo(31);

        String html = mvc.perform(get("/")).andReturn().getResponse().getContentAsString();
        String sidebar = html.substring(html.indexOf("<h3>인기 글</h3>"), html.indexOf("<h3>인기 블로그</h3>"));
        assertThat(sidebar).contains("조회수 많은 인기 글");
        assertThat(sidebar.indexOf("<li>")).isLessThan(sidebar.indexOf("조회수 많은 인기 글"));
        assertThat(sidebar.indexOf("조회수 많은 인기 글")).isLessThan(sidebar.indexOf("<li>", sidebar.indexOf("<li>") + 1));
    }

    @Test
    void 좋아요를_누르고_다시_누르면_취소된다() throws Exception {
        String location = mvc.perform(post("/posts/new").with(csrf()).with(user("alice"))
                        .param("title", "좋아요 테스트 글").param("content", "본문"))
                .andReturn().getResponse().getRedirectedUrl();

        mvc.perform(post(location + "/like").with(csrf()).with(user("bob")).accept("application/json"))
                .andExpect(jsonPath("$.liked").value(true))
                .andExpect(jsonPath("$.count").value(1));
        mvc.perform(post(location + "/like").with(csrf()).with(user("alice")).accept("application/json"))
                .andExpect(jsonPath("$.count").value(2));
        mvc.perform(get(location).with(user("bob")))
                .andExpect(content().string(containsString("like-btn liked")));

        // 다시 누르면 취소
        mvc.perform(post(location + "/like").with(csrf()).with(user("bob")).accept("application/json"))
                .andExpect(jsonPath("$.liked").value(false))
                .andExpect(jsonPath("$.count").value(1));
        // 스크립트 없이 폼으로 보내면 글로 돌아간다
        mvc.perform(post(location + "/like").with(csrf()).with(user("bob")))
                .andExpect(status().isSeeOther())
                .andExpect(redirectedUrl(location + "#like"));
        // 로그인하지 않으면 누를 수 없다
        mvc.perform(post(location + "/like").with(csrf())).andExpect(redirectedUrlPattern("**/login"));
        // 좋아요가 있는 글도 지울 수 있다
        mvc.perform(post(location + "/delete").with(csrf()).with(user("alice"))).andExpect(status().is3xxRedirection());
    }

    @Test
    void 남의_블로그에서_공용_개인_미분류_카테고리별로_글을_본다() throws Exception {
        if (!userService.exists("erin")) {
            userService.register("erin", "password123", "USER");
        }
        mvc.perform(post("/manage/blog/categories").with(csrf()).with(user("erin")).param("name", "요리"));
        Long cook = categoryRepository.findByOwnerUsernameAndName("erin", "요리").orElseThrow().getId();
        Long shared = categoryRepository.findByOwnerIsNullAndName("자유").orElseThrow().getId();

        writeIn("erin", "김치찌개 레시피", cook);
        writeIn("erin", "공용 카테고리에 쓴 글", shared);
        writeIn("erin", "분류 없는 글", null);

        mvc.perform(get("/blog/erin"))
                .andExpect(content().string(containsString("category=" + cook)))
                .andExpect(content().string(containsString("category=" + shared)))
                .andExpect(content().string(containsString("category=0")));

        mvc.perform(get("/blog/erin").param("category", shared.toString()))
                .andExpect(content().string(containsString("공용 카테고리에 쓴 글")))
                .andExpect(content().string(not(containsString("김치찌개 레시피"))));
        mvc.perform(get("/blog/erin").param("category", "0"))
                .andExpect(content().string(containsString("분류 없는 글")))
                .andExpect(content().string(not(containsString("김치찌개 레시피"))))
                .andExpect(content().string(not(containsString("공용 카테고리에 쓴 글"))));
        mvc.perform(get("/blog/erin").param("category", cook.toString()))
                .andExpect(content().string(containsString("김치찌개 레시피")))
                .andExpect(content().string(containsString("1개의 글")));
    }

    private void writeIn(String username, String title, Long categoryId) throws Exception {
        var request = post("/posts/new").with(csrf()).with(user(username)).param("title", title).param("content", "본문");
        if (categoryId != null) {
            request.param("categoryId", categoryId.toString());
        }
        mvc.perform(request).andExpect(status().is3xxRedirection());
    }

    @Test
    void 관리자가_공지로_등록한_글이_사이드바_공지사항에_나온다() throws Exception {
        String location = mvc.perform(post("/posts/new").with(csrf()).with(user("alice"))
                        .param("title", "서버 점검 안내").param("content", "본문"))
                .andReturn().getResponse().getRedirectedUrl();
        String id = location.substring(location.lastIndexOf('/') + 1);

        // 일반 회원은 공지로 올릴 수 없다
        mvc.perform(post("/manage/posts/" + id + "/notice").with(csrf()).with(user("alice")).param("notice", "true"))
                .andExpect(status().isForbidden());

        mvc.perform(post("/manage/posts/" + id + "/notice").with(csrf()).with(user("admin").roles("ADMIN"))
                        .param("notice", "true").param("back", "true"))
                .andExpect(redirectedUrl("/posts/" + id));
        String html = mvc.perform(get("/")).andReturn().getResponse().getContentAsString();
        String notices = html.substring(html.indexOf("<h3>공지사항</h3>"), html.indexOf("blog-about"));
        assertThat(notices).contains("서버 점검 안내");

        mvc.perform(post("/manage/posts/" + id + "/notice").with(csrf()).with(user("admin").roles("ADMIN"))
                .param("notice", "false"));
        html = mvc.perform(get("/")).andReturn().getResponse().getContentAsString();
        notices = html.substring(html.indexOf("<h3>공지사항</h3>"), html.indexOf("blog-about"));
        assertThat(notices).doesNotContain("서버 점검 안내");
    }

    @Test
    void 로그인하면_사이드바에_내_블로그_카드가_나온다() throws Exception {
        mvc.perform(post("/manage/blog").with(csrf()).with(user("alice")).param("title", "앨리스 노트").param("intro", ""));
        String html = mvc.perform(get("/").with(user("alice"))).andReturn().getResponse().getContentAsString();
        String card = html.substring(html.indexOf("my-blog"), html.indexOf("<h3>인기 글</h3>"));
        assertThat(card).contains("앨리스 노트", "조회수", "구독자", "/manage", "/posts/new", "/blog/alice");

        mvc.perform(get("/")).andExpect(content().string(containsString("로그인하면 나만의 블로그를 만들 수 있어요.")));
        mvc.perform(post("/manage/blog").with(csrf()).with(user("alice")).param("title", "").param("intro", ""));
    }

    /** 사이드바(인기 글 등)에도 글 제목이 나오므로 본문 영역만 잘라서 순서를 본다 */
    private static String mainContent(String html) {
        return html.substring(html.indexOf("<section class=\"content\">"));
    }

    private void write(String username, String title, String content) throws Exception {
        mvc.perform(post("/posts/new").with(csrf()).with(user(username))
                .param("title", title).param("content", content));
    }

    @Test
    void 내_블로그는_구독할_수_없고_비로그인은_구독할_수_없다() throws Exception {
        mvc.perform(post("/blog/alice/subscribe").with(csrf()).with(user("alice")))
                .andExpect(flash().attribute("error", "내 블로그는 구독할 수 없습니다."));
        mvc.perform(post("/blog/alice/subscribe").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        mvc.perform(get("/feed")).andExpect(redirectedUrlPattern("**/login"));
        // 블로그 목록과 블로그는 누구나 본다
        mvc.perform(get("/blogs")).andExpect(status().isOk());
        mvc.perform(get("/blog/alice")).andExpect(status().isOk());
    }
}
