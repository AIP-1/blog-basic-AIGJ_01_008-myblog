package com.example.blog;

import com.example.blog.domain.Category;
import com.example.blog.repository.CategoryRepository;
import com.example.blog.repository.CommentRepository;
import com.example.blog.repository.PostRepository;
import com.example.blog.repository.SubscriptionRepository;
import com.example.blog.service.NotificationService;
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
    @Autowired CommentRepository commentRepository;
    @Autowired NotificationService notificationService;

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
        // 글 목록에도 좋아요 수가 보인다
        String html = mainContent(mvc.perform(get("/").param("q", "좋아요 테스트 글"))
                .andReturn().getResponse().getContentAsString());
        assertThat(html).contains("♥ <span>2</span>");

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

    @Test
    void 댓글에_답글을_달고_지울_수_있다() throws Exception {
        String location = mvc.perform(post("/posts/new").with(csrf()).with(user("alice"))
                        .param("title", "답글 테스트 글").param("content", "본문"))
                .andReturn().getResponse().getRedirectedUrl();
        long postId = Long.parseLong(location.substring(location.lastIndexOf('/') + 1));

        mvc.perform(post(location + "/comments").with(csrf()).with(user("bob")).param("content", "원 댓글"));
        var parent = commentRepository.findAll().stream()
                .filter(c -> c.getPost().getId() == postId && !c.isReply()).findFirst().orElseThrow();

        mvc.perform(post(location + "/comments").with(csrf()).with(user("alice"))
                        .param("content", "첫 답글").param("parentId", parent.getId().toString()))
                .andExpect(redirectedUrl(location + "#comment-" + parent.getId()));
        var reply = commentRepository.findAll().stream()
                .filter(c -> c.isReply() && c.getContent().equals("첫 답글")).findFirst().orElseThrow();
        // 답글에 답글을 달면 원 댓글 아래에 붙는다
        mvc.perform(post(location + "/comments").with(csrf()).with(user("bob"))
                .param("content", "@alice 답글의 답글").param("parentId", reply.getId().toString()));
        assertThat(commentRepository.findAll().stream()
                .filter(c -> c.getContent().equals("@alice 답글의 답글")).findFirst().orElseThrow()
                .getParent().getId()).isEqualTo(parent.getId());

        mvc.perform(get(location))
                .andExpect(content().string(containsString("class=\"replies\"")))
                .andExpect(content().string(containsString("댓글 <span>3</span>")));

        // 답글이 있는 댓글을 지우면 '삭제된 댓글'로 남고 답글은 그대로
        mvc.perform(post("/comments/" + parent.getId() + "/delete").with(csrf()).with(user("bob")));
        mvc.perform(get(location))
                .andExpect(content().string(containsString("삭제된 댓글입니다.")))
                .andExpect(content().string(containsString("첫 답글")))
                .andExpect(content().string(containsString("댓글 <span>2</span>")));

        // 다른 사람의 답글은 지울 수 없다
        mvc.perform(post("/comments/" + reply.getId() + "/delete").with(csrf()).with(user("bob")))
                .andExpect(status().isForbidden());

        // 답글과 댓글이 남아 있어도 글을 지울 수 있다
        mvc.perform(post(location + "/delete").with(csrf()).with(user("alice"))).andExpect(status().is3xxRedirection());
        assertThat(commentRepository.findAll()).noneMatch(c -> c.getPost().getId() == postId);
    }

    @Test
    void 삭제된_댓글의_마지막_답글을_지우면_댓글도_사라진다() throws Exception {
        String location = mvc.perform(post("/posts/new").with(csrf()).with(user("alice"))
                        .param("title", "정리 테스트 글").param("content", "본문"))
                .andReturn().getResponse().getRedirectedUrl();
        mvc.perform(post(location + "/comments").with(csrf()).with(user("bob")).param("content", "곧 지울 댓글"));
        Long parentId = commentRepository.findAll().stream()
                .filter(c -> c.getContent().equals("곧 지울 댓글")).findFirst().orElseThrow().getId();
        mvc.perform(post(location + "/comments").with(csrf()).with(user("alice"))
                .param("content", "하나뿐인 답글").param("parentId", parentId.toString()));
        Long replyId = commentRepository.findAll().stream()
                .filter(c -> c.getContent().equals("하나뿐인 답글")).findFirst().orElseThrow().getId();

        mvc.perform(post("/comments/" + parentId + "/delete").with(csrf()).with(user("bob")));
        assertThat(commentRepository.findById(parentId)).isPresent();
        mvc.perform(post("/comments/" + replyId + "/delete").with(csrf()).with(user("alice")));
        assertThat(commentRepository.findById(parentId)).isEmpty();
        assertThat(commentRepository.findById(replyId)).isEmpty();
    }

    @Test
    void 내_글에_댓글이나_내_댓글에_답글이_달리면_알림이_온다() throws Exception {
        if (!userService.exists("frank")) {
            userService.register("frank", "password123", "USER");
        }
        long before = notificationService.unreadCount("frank");
        String location = mvc.perform(post("/posts/new").with(csrf()).with(user("frank"))
                        .param("title", "알림 테스트 글").param("content", "본문"))
                .andReturn().getResponse().getRedirectedUrl();

        // 내가 단 댓글은 알림이 없다
        mvc.perform(post(location + "/comments").with(csrf()).with(user("frank")).param("content", "내 댓글"));
        assertThat(notificationService.unreadCount("frank")).isEqualTo(before);

        // bob 이 댓글 → frank(글쓴이)에게 알림
        mvc.perform(post(location + "/comments").with(csrf()).with(user("bob")).param("content", "bob 의 댓글"));
        assertThat(notificationService.unreadCount("frank")).isEqualTo(before + 1);
        Long bobComment = commentRepository.findAll().stream()
                .filter(c -> c.getContent().equals("bob 의 댓글")).findFirst().orElseThrow().getId();

        // alice 가 bob 댓글에 답글 → bob 에게 답글 알림, frank 에게 댓글 알림 (각 한 번)
        long bobBefore = notificationService.unreadCount("bob");
        mvc.perform(post(location + "/comments").with(csrf()).with(user("alice"))
                .param("content", "alice 의 답글").param("parentId", bobComment.toString()));
        assertThat(notificationService.unreadCount("bob")).isEqualTo(bobBefore + 1);
        assertThat(notificationService.unreadCount("frank")).isEqualTo(before + 2);

        // 상단 🔔 에 안 읽은 수와 최근 알림이 보인다
        mvc.perform(get("/").with(user("frank")))
                .andExpect(content().string(containsString("class=\"noti-badge\">" + (before + 2) + "<")))
                .andExpect(content().string(containsString("님이 내 글에 댓글을 남겼어요")))
                .andExpect(content().string(containsString("alice 의 답글")));

        // 알림을 누르면 그 댓글로 이동하고 읽음 처리
        Long notiId = notificationService.recent("frank").get(0).getId();
        String target = mvc.perform(get("/notifications/" + notiId).with(user("frank")))
                .andReturn().getResponse().getRedirectedUrl();
        assertThat(target).startsWith(location + "#comment-");
        assertThat(notificationService.unreadCount("frank")).isEqualTo(before + 1);
        // 남의 알림은 열 수 없다
        mvc.perform(get("/notifications/" + notiId).with(user("bob"))).andExpect(status().isNotFound());

        mvc.perform(post("/notifications/read-all").with(csrf()).with(user("frank")));
        assertThat(notificationService.unreadCount("frank")).isZero();
        mvc.perform(get("/notifications").with(user("frank"))).andExpect(status().isOk());

        // 알림이 달린 글도 지울 수 있다
        mvc.perform(post(location + "/delete").with(csrf()).with(user("frank"))).andExpect(status().is3xxRedirection());
    }

    @Test
    void 관리자가_정지한_회원은_로그인할_수_없고_로그인_중이면_쫓겨난다() throws Exception {
        if (!userService.exists("spammer")) {
            userService.register("spammer", "password123", "USER");
        }
        // 일반 회원은 회원 관리에 못 들어간다
        mvc.perform(get("/manage/users").with(user("alice"))).andExpect(status().isForbidden());

        mvc.perform(post("/manage/users/spammer/ban").with(csrf()).with(user("admin").roles("ADMIN"))
                        .param("reason", "광고 도배"))
                .andExpect(flash().attribute("message", "spammer 님의 이용을 정지했습니다."));
        assertThat(userService.get("spammer").getBanReason()).isEqualTo("광고 도배");

        mvc.perform(post("/login").with(csrf()).param("username", "spammer").param("password", "password123"))
                .andExpect(redirectedUrl("/login?banned"));
        // 이미 로그인한 세션도 다음 요청에서 로그아웃
        mvc.perform(get("/manage").with(user("spammer"))).andExpect(redirectedUrl("/login?banned"));
        mvc.perform(get("/login").param("banned", "")).andExpect(content().string(containsString("이용이 정지된 계정입니다")));

        // 관리자는 정지할 수 없다
        mvc.perform(post("/manage/users/admin/ban").with(csrf()).with(user("admin").roles("ADMIN")))
                .andExpect(flash().attribute("error", "관리자 계정은 정지할 수 없습니다."));

        mvc.perform(post("/manage/users/spammer/unban").with(csrf()).with(user("admin").roles("ADMIN")));
        mvc.perform(post("/login").with(csrf()).param("username", "spammer").param("password", "password123"))
                .andExpect(redirectedUrl("/"));
        mvc.perform(get("/manage/users").with(user("admin").roles("ADMIN")).param("q", "spam"))
                .andExpect(content().string(containsString("spammer")));
    }

    @Test
    void 차단하면_글이_안_보이고_차단당한_사람은_댓글_구독을_못_한다() throws Exception {
        for (String name : new String[]{"gina", "troll"}) {
            if (!userService.exists(name)) {
                userService.register(name, "password123", "USER");
            }
        }
        write("troll", "트롤의 차단 테스트 글", "본문");
        String ginaPost = mvc.perform(post("/posts/new").with(csrf()).with(user("gina"))
                        .param("title", "지나의 글").param("content", "본문"))
                .andReturn().getResponse().getRedirectedUrl();
        mvc.perform(post(ginaPost + "/comments").with(csrf()).with(user("troll")).param("content", "트롤의 댓글"));
        mvc.perform(post("/blog/gina/subscribe").with(csrf()).with(user("troll")));

        mvc.perform(post("/blog/troll/block").with(csrf()).with(user("gina")))
                .andExpect(redirectedUrl("/blog/troll"));

        // gina 에게는 troll 의 글이 목록에서 빠지고, 댓글은 접힌다
        mvc.perform(get("/").with(user("gina")).param("q", "트롤의 차단 테스트"))
                .andExpect(content().string(containsString("검색 결과 <span>0</span>건")));
        mvc.perform(get("/").param("q", "트롤의 차단 테스트"))
                .andExpect(content().string(containsString("검색 결과 <span>1</span>건")));
        mvc.perform(get(ginaPost).with(user("gina")))
                .andExpect(content().string(containsString("차단한 사용자의 댓글입니다.")))
                .andExpect(content().string(not(containsString("트롤의 댓글"))));
        mvc.perform(get("/blog/troll").with(user("gina")))
                .andExpect(content().string(containsString("차단한 사용자의 블로그예요")));

        // troll 은 gina 글에 댓글을 달 수 없고, 구독도 끊겼다
        long before = commentRepository.count();
        mvc.perform(post(ginaPost + "/comments").with(csrf()).with(user("troll")).param("content", "또 댓글"))
                .andExpect(flash().attribute("commentError", "글쓴이가 차단해 이 글에는 댓글을 달 수 없어요."));
        assertThat(commentRepository.count()).isEqualTo(before);
        assertThat(subscriptionRepository.findBySubscriberUsernameOrderByCreatedAtDesc("troll")).isEmpty();
        mvc.perform(post("/blog/gina/subscribe").with(csrf()).with(user("troll")))
                .andExpect(flash().attribute("error", "구독할 수 없는 블로그입니다."));

        mvc.perform(get("/manage/blocks").with(user("gina"))).andExpect(content().string(containsString("troll")));
        mvc.perform(post("/manage/blocks/troll/delete").with(csrf()).with(user("gina")));
        mvc.perform(get("/").with(user("gina")).param("q", "트롤의 차단 테스트"))
                .andExpect(content().string(containsString("검색 결과 <span>1</span>건")));
    }

    @Test
    void 글_관리에서_검색할_수_있다() throws Exception {
        write("bob", "관리검색 사과 글", "본문");
        write("bob", "다른 글 제목", "내용에 관리검색 사과");
        write("alice", "앨리스 관리검색 사과", "본문");

        // 일반 회원은 자기 글에서만 찾는다
        String mine = mvc.perform(get("/manage/posts").with(user("bob")).param("q", "관리검색 사과"))
                .andExpect(content().string(containsString("검색 결과 <span>2</span>건")))
                .andReturn().getResponse().getContentAsString();
        assertThat(mine).doesNotContain("앨리스 관리검색 사과");
        // 관리자는 모든 글에서, 작성자 아이디로도 찾는다
        mvc.perform(get("/manage/posts").with(user("admin").roles("ADMIN")).param("q", "관리검색 사과"))
                .andExpect(content().string(containsString("검색 결과 <span>3</span>건")));
        mvc.perform(get("/manage/posts").with(user("admin").roles("ADMIN")).param("q", "bob"))
                .andExpect(content().string(containsString("관리검색 사과 글")));
    }

    @Test
    void 달력에_글_쓴_날이_표시되고_날짜를_누르면_그날_글만_나온다() throws Exception {
        write("alice", "달력 테스트 오늘 글", "본문");
        java.time.LocalDate today = java.time.LocalDate.now();

        mvc.perform(get("/calendar").param("month", java.time.YearMonth.from(today).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days['" + today.getDayOfMonth() + "']").isNumber());

        String day = mainContent(mvc.perform(get("/").param("date", today.toString()))
                .andReturn().getResponse().getContentAsString());
        assertThat(day).contains("달력 테스트 오늘 글", "에 쓴 글");
        String longAgo = mainContent(mvc.perform(get("/").param("date", today.minusYears(5).toString()))
                .andReturn().getResponse().getContentAsString());
        assertThat(longAgo).doesNotContain("달력 테스트 오늘 글").contains("에 쓴 글");
    }

    @Test
    void 블로그_관리에_들어가면_통계가_나온다() throws Exception {
        if (!userService.exists("hana")) {
            userService.register("hana", "password123", "USER");
        }
        String location = mvc.perform(post("/posts/new").with(csrf()).with(user("hana"))
                        .param("title", "통계 테스트 글").param("content", "본문"))
                .andReturn().getResponse().getRedirectedUrl();
        // 다른 사람 두 명이 글을 읽는다 → 블로그 방문 2, 글 조회 2
        mvc.perform(get(location).session(new MockHttpSession()));
        mvc.perform(get(location).session(new MockHttpSession()));

        String html = mvc.perform(get("/manage").with(user("hana")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("최근 7일 방문자", "통계 테스트 글", "오늘 방문", "class=\"bar-chart\"");
        assertThat(html.substring(html.indexOf("오늘 방문"))).contains("<strong class=\"stat-value\">2</strong>");
        assertThat(html).contains("글 조회 2");
    }

    @Test
    void 구독한_블로그에_새_글이_공개되면_알림이_온다() throws Exception {
        for (String name : new String[]{"ivy", "jack"}) {
            if (!userService.exists(name)) {
                userService.register(name, "password123", "USER");
            }
        }
        mvc.perform(post("/blog/ivy/subscribe").with(csrf()).with(user("jack")));
        long before = notificationService.unreadCount("jack");

        // 비공개로 쓰면 알림 없음 → 공개로 바꾸는 순간 한 번
        String location = mvc.perform(post("/posts/new").with(csrf()).with(user("ivy"))
                        .param("title", "아이비의 새 글").param("content", "본문").param("status", "PRIVATE"))
                .andReturn().getResponse().getRedirectedUrl();
        String id = location.substring(location.lastIndexOf('/') + 1);
        assertThat(notificationService.unreadCount("jack")).isEqualTo(before);

        mvc.perform(post("/manage/posts/" + id + "/status").with(csrf()).with(user("ivy")).param("status", "PUBLIC"));
        assertThat(notificationService.unreadCount("jack")).isEqualTo(before + 1);
        // 다시 비공개 → 공개해도 또 보내지 않는다
        mvc.perform(post("/manage/posts/" + id + "/status").with(csrf()).with(user("ivy")).param("status", "PRIVATE"));
        mvc.perform(post("/manage/posts/" + id + "/status").with(csrf()).with(user("ivy")).param("status", "PUBLIC"));
        assertThat(notificationService.unreadCount("jack")).isEqualTo(before + 1);

        mvc.perform(get("/").with(user("jack")))
                .andExpect(content().string(containsString("님이 새 글을 올렸어요")))
                .andExpect(content().string(containsString("아이비의 새 글")));
        Long notiId = notificationService.recent("jack").get(0).getId();
        mvc.perform(get("/notifications/" + notiId).with(user("jack"))).andExpect(redirectedUrl(location));

        // 바로 공개로 쓴 글도 알림
        write("ivy", "아이비의 두 번째 글", "본문");
        assertThat(notificationService.unreadCount("jack")).isEqualTo(before + 1);
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
