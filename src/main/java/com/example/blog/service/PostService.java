package com.example.blog.service;

import com.example.blog.domain.Category;
import com.example.blog.domain.Comment;
import com.example.blog.domain.Post;
import com.example.blog.domain.User;
import com.example.blog.repository.CommentRepository;
import com.example.blog.repository.PostRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class PostService {

    private static final int PAGE_SIZE = 10;

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserService userService;

    public PostService(PostRepository postRepository, CommentRepository commentRepository, UserService userService) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.userService = userService;
    }

    public Page<Post> search(Category category, String keyword, int page) {
        String q = keyword == null ? "" : keyword.trim();
        PageRequest pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by(Sort.Direction.DESC, "id"));
        return postRepository.search(category, q, pageable);
    }

    public List<Post> curriculum() {
        return postRepository.findBySeqNotNullOrderBySeqAsc();
    }

    public Post get(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "글을 찾을 수 없습니다."));
    }

    public Optional<Post> previous(Post post) {
        return post.getSeq() == null ? Optional.empty() : postRepository.findFirstBySeqLessThanOrderBySeqDesc(post.getSeq());
    }

    public Optional<Post> next(Post post) {
        return post.getSeq() == null ? Optional.empty() : postRepository.findFirstBySeqGreaterThanOrderBySeqAsc(post.getSeq());
    }

    @Transactional
    public Post create(String title, String content, Category category, String username) {
        User author = userService.get(username);
        return postRepository.save(new Post(title, content, category, null, author));
    }

    @Transactional
    public void update(Long id, String title, String content, Category category, Authentication auth) {
        Post post = get(id);
        checkEditable(post.isWrittenBy(auth.getName()), auth);
        post.update(title, content, category);
    }

    @Transactional
    public void delete(Long id, Authentication auth) {
        Post post = get(id);
        checkEditable(post.isWrittenBy(auth.getName()), auth);
        postRepository.delete(post);
    }

    @Transactional
    public void addComment(Long postId, String content, String username) {
        Post post = get(postId);
        commentRepository.save(new Comment(content, post, userService.get(username)));
    }

    /** 삭제 후 돌아갈 글 id 를 반환 */
    @Transactional
    public Long deleteComment(Long commentId, Authentication auth) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."));
        checkEditable(comment.isWrittenBy(auth.getName()), auth);
        Long postId = comment.getPost().getId();
        comment.getPost().getComments().remove(comment);
        commentRepository.delete(comment);
        return postId;
    }

    public boolean canEdit(Post post, Authentication auth) {
        return auth != null && auth.isAuthenticated() && (post.isWrittenBy(auth.getName()) || isAdmin(auth));
    }

    private void checkEditable(boolean isOwner, Authentication auth) {
        if (!isOwner && !isAdmin(auth)) {
            throw new AccessDeniedException("권한이 없습니다.");
        }
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
