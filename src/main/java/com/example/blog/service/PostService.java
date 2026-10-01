package com.example.blog.service;

import com.example.blog.domain.Comment;
import com.example.blog.domain.Post;
import com.example.blog.domain.PostLike;
import com.example.blog.domain.PostStatus;
import com.example.blog.domain.User;
import com.example.blog.repository.CommentRepository;
import com.example.blog.repository.PostLikeRepository;
import com.example.blog.repository.PostRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class PostService {

    private static final int PAGE_SIZE = 10;
    private static final int MANAGE_PAGE_SIZE = 20;
    private static final String UNTITLED = "(제목 없음)";

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserService userService;
    private final CategoryService categoryService;

    public PostService(PostRepository postRepository, CommentRepository commentRepository,
                       PostLikeRepository postLikeRepository, UserService userService, CategoryService categoryService) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.postLikeRepository = postLikeRepository;
        this.userService = userService;
        this.categoryService = categoryService;
    }

    /** 목록은 최신순, 검색하면 작성자 → 제목 → 내용 일치 순 (정렬은 쿼리 안에 있음) */
    public Page<Post> search(Long categoryId, String keyword, int page) {
        String q = keyword == null ? "" : keyword.trim().toLowerCase();
        return postRepository.searchPublic(categoryId, q, PageRequest.of(Math.max(page, 0), PAGE_SIZE));
    }

    /** 사이드바 공지사항 (공개 글 중 최신 5개) */
    public List<Post> notices() {
        return postRepository.findTop5ByNoticeTrueAndStatusOrderByCreatedAtDesc(PostStatus.PUBLIC);
    }

    /** 공지 등록/해제 (관리자만: SecurityConfig 에서 막는다) */
    @Transactional
    public void changeNotice(Long id, boolean notice, Authentication auth) {
        if (!isAdmin(auth)) {
            throw new AccessDeniedException("권한이 없습니다.");
        }
        get(id).changeNotice(notice);
    }

    /** 사이드바 인기 글 (공개 글 중 조회수 상위 5개) */
    public List<Post> popular() {
        return postRepository.findTop5ByStatusOrderByViewCountDescCreatedAtDesc(PostStatus.PUBLIC);
    }

    /**
     * 조회수 기록. 같은 세션에서는 글마다 한 번만 세고, 작성자 본인이 본 것은 세지 않는다.
     * viewed 는 세션에 보관하는 '이미 센 글' 목록이다.
     */
    @Transactional
    public void recordView(Long id, String viewer, Set<Long> viewed) {
        Post post = get(id);
        if (!post.isPublic() || post.isWrittenBy(viewer == null ? "" : viewer) || !viewed.add(id)) {
            return;
        }
        post.increaseViewCount();
    }

    public List<Post> curriculum() {
        return postRepository.findBySeqNotNullAndStatusOrderBySeqAsc(PostStatus.PUBLIC);
    }

    public Post get(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "글을 찾을 수 없습니다."));
    }

    /** 비공개·임시저장 글은 작성자와 관리자만 볼 수 있다. 다른 사람에게는 없는 글처럼 404 */
    public Post getVisible(Long id, Authentication auth) {
        Post post = get(id);
        if (!post.isPublic() && !canEdit(post, auth)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "글을 찾을 수 없습니다.");
        }
        return post;
    }

    public Optional<Post> previous(Post post) {
        return post.getSeq() == null || !post.isPublic() ? Optional.empty()
                : postRepository.findFirstBySeqLessThanAndStatusOrderBySeqDesc(post.getSeq(), PostStatus.PUBLIC);
    }

    public Optional<Post> next(Post post) {
        return post.getSeq() == null || !post.isPublic() ? Optional.empty()
                : postRepository.findFirstBySeqGreaterThanAndStatusOrderBySeqAsc(post.getSeq(), PostStatus.PUBLIC);
    }

    @Transactional
    public Post create(String title, String content, Long categoryId, PostStatus status, String username) {
        User author = userService.get(username);
        return postRepository.save(new Post(title, content, categoryService.findUsable(categoryId, username), status, null, author));
    }

    @Transactional
    public void update(Long id, String title, String content, Long categoryId, PostStatus status, Authentication auth) {
        Post post = get(id);
        checkEditable(post, auth);
        post.update(title, content, categoryService.findUsable(categoryId, post.getAuthor().getUsername()));
        post.changeStatus(status);
    }

    /**
     * 임시저장. id 가 없으면 새 임시저장 글을 만든다.
     * 이미 발행한 글은 임시저장으로 덮어쓰지 않는다 (발행본이 바뀌면 안 되므로).
     */
    @Transactional
    public Post saveDraft(Long id, String title, String content, Long categoryId, Authentication auth) {
        String safeTitle = StringUtils.hasText(title) ? title.trim() : UNTITLED;
        String safeContent = content == null ? "" : content;
        if (id == null) {
            return postRepository.save(new Post(safeTitle, safeContent, categoryService.findUsable(categoryId, auth.getName()),
                    PostStatus.DRAFT, null, userService.get(auth.getName())));
        }
        Post post = get(id);
        checkEditable(post, auth);
        if (!post.isDraft()) {
            throw new IllegalStateException("이미 발행한 글은 임시저장할 수 없습니다.");
        }
        post.update(safeTitle, safeContent, categoryService.findUsable(categoryId, post.getAuthor().getUsername()));
        return post;
    }

    public List<Post> drafts(String username) {
        return postRepository.findByAuthorUsernameAndStatusOrderByUpdatedAtDesc(username, PostStatus.DRAFT);
    }

    /** 관리 페이지: 일반 사용자는 자기 글만, 관리자는 모든 글 */
    public Page<Post> manageList(PostStatus status, int page, Authentication auth) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), MANAGE_PAGE_SIZE, Sort.by(Sort.Direction.DESC, "updatedAt", "id"));
        return postRepository.findForManage(manageScope(auth), status, pageable);
    }

    /** 상태별 글 수 (키: PUBLIC, PRIVATE, DRAFT) */
    public Map<String, Long> manageCounts(Authentication auth) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (PostStatus status : PostStatus.values()) {
            counts.put(status.name(), postRepository.countForManage(manageScope(auth), status));
        }
        return counts;
    }

    @Transactional
    public void changeStatus(Long id, PostStatus status, Authentication auth) {
        Post post = get(id);
        checkEditable(post, auth);
        post.changeStatus(status);
    }

    @Transactional
    public void delete(Long id, Authentication auth) {
        Post post = get(id);
        checkEditable(post, auth);
        postLikeRepository.deleteByPost(post);
        postRepository.delete(post);
    }

    // ===== 좋아요 =====

    public record LikeState(boolean liked, long count) {
    }

    public LikeState likeState(Post post, Authentication auth) {
        boolean liked = auth != null && postLikeRepository.existsByPostAndUserUsername(post, auth.getName());
        return new LikeState(liked, postLikeRepository.countByPost(post));
    }

    /** 좋아요를 누르거나(없으면) 취소한다(있으면). 볼 수 있는 글에만 누를 수 있다 */
    @Transactional
    public LikeState toggleLike(Long postId, Authentication auth) {
        Post post = getVisible(postId, auth);
        postLikeRepository.findByPostAndUserUsername(post, auth.getName())
                .ifPresentOrElse(postLikeRepository::delete,
                        () -> postLikeRepository.save(new PostLike(post, userService.get(auth.getName()))));
        postLikeRepository.flush();
        return likeState(post, auth);
    }

    @Transactional
    public void addComment(Long postId, String content, Authentication auth) {
        Post post = getVisible(postId, auth);
        commentRepository.save(new Comment(content, post, userService.get(auth.getName())));
    }

    /** 삭제 후 돌아갈 글 id 를 반환 */
    @Transactional
    public Long deleteComment(Long commentId, Authentication auth) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."));
        if (!comment.isWrittenBy(auth.getName()) && !isAdmin(auth)) {
            throw new AccessDeniedException("권한이 없습니다.");
        }
        Long postId = comment.getPost().getId();
        comment.getPost().getComments().remove(comment);
        commentRepository.delete(comment);
        return postId;
    }

    public boolean canEdit(Post post, Authentication auth) {
        return auth != null && auth.isAuthenticated() && (post.isWrittenBy(auth.getName()) || isAdmin(auth));
    }

    private void checkEditable(Post post, Authentication auth) {
        if (!canEdit(post, auth)) {
            throw new AccessDeniedException("권한이 없습니다.");
        }
    }

    private String manageScope(Authentication auth) {
        return isAdmin(auth) ? null : auth.getName();
    }

    public boolean isAdmin(Authentication auth) {
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
