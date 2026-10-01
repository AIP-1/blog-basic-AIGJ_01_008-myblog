package com.example.blog.service;

import com.example.blog.domain.Comment;
import com.example.blog.domain.Post;
import com.example.blog.domain.PostLike;
import com.example.blog.domain.PostStatus;
import com.example.blog.domain.User;
import com.example.blog.repository.CommentRepository;
import com.example.blog.repository.NotificationRepository;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

@Service
@Transactional(readOnly = true)
public class PostService {

    private static final int PAGE_SIZE = 10;
    private static final int MANAGE_PAGE_SIZE = 20;
    private static final String UNTITLED = "(제목 없음)";

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final BlockService blockService;
    private final StatsService statsService;
    private final UserService userService;
    private final CategoryService categoryService;

    public PostService(PostRepository postRepository, CommentRepository commentRepository,
                       PostLikeRepository postLikeRepository, NotificationRepository notificationRepository,
                       NotificationService notificationService, BlockService blockService,
                       StatsService statsService, UserService userService, CategoryService categoryService) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.postLikeRepository = postLikeRepository;
        this.notificationRepository = notificationRepository;
        this.notificationService = notificationService;
        this.blockService = blockService;
        this.statsService = statsService;
        this.userService = userService;
        this.categoryService = categoryService;
    }

    /** 목록은 최신순, 검색하면 작성자 → 제목 → 내용 일치 순 (정렬은 쿼리 안에 있음) */
    public Page<Post> search(Long categoryId, String keyword, int page, String viewer) {
        return search(categoryId, keyword, null, page, viewer);
    }

    /** date 가 있으면 그날 쓴 글만 (달력) */
    public Page<Post> search(Long categoryId, String keyword, LocalDate date, int page, String viewer) {
        String q = keyword == null ? "" : keyword.trim().toLowerCase();
        LocalDateTime from = date == null ? null : date.atStartOfDay();
        LocalDateTime to = date == null ? null : date.plusDays(1).atStartOfDay();
        return postRepository.searchPublic(categoryId, q, viewer == null ? "" : viewer, from, to,
                PageRequest.of(Math.max(page, 0), PAGE_SIZE));
    }

    /** 달력: 그 달에 공개 글을 쓴 날짜별 글 수 (키: 일) */
    public Map<Integer, Long> postCountsByDay(YearMonth month) {
        Map<Integer, Long> counts = new TreeMap<>();
        postRepository.findPublicCreatedAtBetween(month.atDay(1).atStartOfDay(), month.plusMonths(1).atDay(1).atStartOfDay())
                .forEach(t -> counts.merge(t.getDayOfMonth(), 1L, Long::sum));
        return counts;
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

    /** 사이드바 인기 글 (공개 글 중 조회수 상위 5개, 내가 차단한 사람의 글은 빼고) */
    public List<Post> popular(String viewer) {
        Set<Long> blocked = blockService.blockedIds(viewer);
        return postRepository.findTop10ByStatusOrderByViewCountDescCreatedAtDesc(PostStatus.PUBLIC).stream()
                .filter(p -> !blocked.contains(p.getAuthor().getId()))
                .limit(5)
                .toList();
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
        statsService.recordPostView(post.getAuthor().getUsername());
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
        Post post = postRepository.save(new Post(title, content, categoryService.findUsable(categoryId, username), status, null, author));
        notifySubscribersIfPublished(post);
        return post;
    }

    @Transactional
    public void update(Long id, String title, String content, Long categoryId, PostStatus status, Authentication auth) {
        Post post = get(id);
        checkEditable(post, auth);
        post.update(title, content, categoryService.findUsable(categoryId, post.getAuthor().getUsername()));
        post.changeStatus(status);
        notifySubscribersIfPublished(post);
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
    public Page<Post> manageList(PostStatus status, String keyword, int page, Authentication auth) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), MANAGE_PAGE_SIZE, Sort.by(Sort.Direction.DESC, "updatedAt", "id"));
        return postRepository.findForManage(manageScope(auth), status, manageKeyword(keyword), pageable);
    }

    /** 상태별 글 수 (키: PUBLIC, PRIVATE, DRAFT). 검색 중이면 검색 결과 안에서 센다 */
    public Map<String, Long> manageCounts(String keyword, Authentication auth) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (PostStatus status : PostStatus.values()) {
            counts.put(status.name(), postRepository.countForManage(manageScope(auth), status, manageKeyword(keyword)));
        }
        return counts;
    }

    private static String manageKeyword(String keyword) {
        return keyword == null ? "" : keyword.trim().toLowerCase();
    }

    @Transactional
    public void changeStatus(Long id, PostStatus status, Authentication auth) {
        Post post = get(id);
        checkEditable(post, auth);
        post.changeStatus(status);
        notifySubscribersIfPublished(post);
    }

    /** 글이 처음 공개되는 순간 구독자에게 알린다 (다시 공개해도 한 번만) */
    private void notifySubscribersIfPublished(Post post) {
        if (post.markSubscribersNotifiedIfPublic()) {
            notificationService.notifyNewPost(post);
        }
    }

    @Transactional
    public void delete(Long id, Authentication auth) {
        Post post = get(id);
        checkEditable(post, auth);
        postLikeRepository.deleteByPost(post);
        notificationRepository.deleteByPost(post);
        // 답글이 원 댓글을 가리키므로 답글부터 지운 뒤 글(과 남은 댓글)을 지운다
        post.getComments().stream().filter(Comment::isReply).toList().forEach(this::removeComment);
        commentRepository.flush();
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

    /** parentId 가 있으면 그 댓글의 답글. 답글에 답글을 달면 원래 댓글 아래에 붙는다 */
    @Transactional
    public void addComment(Long postId, String content, Long parentId, Authentication auth) {
        Post post = getVisible(postId, auth);
        Comment parent = null;
        Comment target = null;
        if (parentId != null) {
            parent = target = findComment(parentId);
            if (!parent.getPost().getId().equals(post.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "다른 글의 댓글입니다.");
            }
            if (parent.isReply()) {
                parent = parent.getParent();
            }
            if (parent.isDeleted()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "삭제된 댓글에는 답글을 달 수 없습니다.");
            }
        }
        checkNotBlockedBy(post.getAuthor(), auth, "글쓴이가 차단해 이 글에는 댓글을 달 수 없어요.");
        if (parent != null) {
            checkNotBlockedBy(target.getAuthor(), auth, "댓글 작성자가 차단해 답글을 달 수 없어요.");
            checkNotBlockedBy(parent.getAuthor(), auth, "댓글 작성자가 차단해 답글을 달 수 없어요.");
        }
        Comment comment = commentRepository.save(new Comment(content, post, userService.get(auth.getName()), parent));
        post.getComments().add(comment);
        if (parent != null) {
            parent.getReplies().add(comment);
        }
        notificationService.notifyNewComment(comment, target);
    }

    /**
     * 삭제 후 돌아갈 글 id 를 반환.
     * 답글이 달린 댓글은 '삭제된 댓글'로만 표시하고, 그런 댓글의 마지막 답글이 지워지면 댓글도 함께 지운다.
     */
    @Transactional
    public Long deleteComment(Long commentId, Authentication auth) {
        Comment comment = findComment(commentId);
        if (!comment.isWrittenBy(auth.getName()) && !isAdmin(auth)) {
            throw new AccessDeniedException("권한이 없습니다.");
        }
        Long postId = comment.getPost().getId();
        if (!comment.getReplies().isEmpty()) {
            comment.markDeleted();
            return postId;
        }
        Comment parent = comment.getParent();
        removeComment(comment);
        if (parent != null && parent.isDeleted() && parent.getReplies().isEmpty()) {
            removeComment(parent);
        }
        return postId;
    }

    /** owner 가 지금 사용자를 차단했다면 IllegalStateException (화면에 메시지로 보여 준다) */
    private void checkNotBlockedBy(User owner, Authentication auth, String message) {
        if (blockService.isBlocked(owner.getUsername(), auth.getName())) {
            throw new IllegalStateException(message);
        }
    }

    private Comment findComment(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."));
    }

    private void removeComment(Comment comment) {
        if (comment.getParent() != null) {
            comment.getParent().getReplies().remove(comment);
        }
        comment.getPost().getComments().remove(comment);
        notificationRepository.deleteByComment(comment);
        commentRepository.delete(comment);
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
