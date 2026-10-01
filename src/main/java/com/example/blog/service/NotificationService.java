package com.example.blog.service;

import com.example.blog.domain.Comment;
import com.example.blog.domain.Notification;
import com.example.blog.domain.Post;
import com.example.blog.domain.Subscription;
import com.example.blog.domain.User;
import com.example.blog.repository.NotificationRepository;
import com.example.blog.repository.SubscriptionRepository;
import com.example.blog.repository.UserBlockRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

@Service
@Transactional(readOnly = true)
public class NotificationService {

    private static final int PAGE_SIZE = 20;

    private final NotificationRepository notificationRepository;
    private final UserBlockRepository blockRepository;
    private final SubscriptionRepository subscriptionRepository;

    public NotificationService(NotificationRepository notificationRepository, UserBlockRepository blockRepository,
                               SubscriptionRepository subscriptionRepository) {
        this.notificationRepository = notificationRepository;
        this.blockRepository = blockRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    /**
     * 새 댓글에 대한 알림을 만든다.
     * - 답글이면: 답글을 단 대상 댓글의 작성자, 원 댓글의 작성자에게 REPLY
     * - 그리고 글 작성자에게 COMMENT (이미 REPLY 를 받았으면 생략)
     * 쓴 사람 본인, 쓴 사람을 차단한 사람에게는 보내지 않고, 한 사람에게 한 번만 보낸다.
     *
     * @param target 사용자가 '답글'을 누른 댓글 (원 댓글이 아니라 답글일 수도 있다). 일반 댓글이면 null
     */
    @Transactional
    public void notifyNewComment(Comment comment, Comment target) {
        Map<Long, Notification> notifications = new LinkedHashMap<>();
        BiConsumer<User, Notification.Type> add = (user, type) -> {
            boolean self = user.getId().equals(comment.getAuthor().getId());
            boolean blocked = blockRepository.existsByBlockerUsernameAndBlockedUsername(
                    user.getUsername(), comment.getAuthor().getUsername());
            if (!self && !blocked) {
                notifications.putIfAbsent(user.getId(), new Notification(user, comment, type));
            }
        };
        if (target != null) {
            add.accept(target.getAuthor(), Notification.Type.REPLY);
            add.accept(comment.getParent().getAuthor(), Notification.Type.REPLY);
        }
        add.accept(comment.getPost().getAuthor(), Notification.Type.COMMENT);
        notificationRepository.saveAll(notifications.values());
    }

    /** 새 글이 처음 공개되면 글쓴이 블로그의 구독자들에게 알린다 */
    @Transactional
    public void notifyNewPost(Post post) {
        List<Notification> notifications = subscriptionRepository.findByBlogOwner(post.getAuthor()).stream()
                .map(Subscription::getSubscriber)
                .filter(u -> !blockRepository.existsByBlockerUsernameAndBlockedUsername(u.getUsername(), post.getAuthor().getUsername()))
                .map(u -> Notification.newPost(u, post))
                .toList();
        notificationRepository.saveAll(notifications);
    }

    public long unreadCount(String username) {
        return notificationRepository.countByRecipientUsernameAndIsReadFalse(username);
    }

    public List<Notification> recent(String username) {
        return notificationRepository.findTop8ByRecipientUsernameOrderByCreatedAtDescIdDesc(username);
    }

    public Page<Notification> list(String username, int page) {
        return notificationRepository.findByRecipientUsernameOrderByCreatedAtDescIdDesc(
                username, PageRequest.of(Math.max(page, 0), PAGE_SIZE));
    }

    /** 알림을 읽음으로 바꾸고, 이동할 주소(글의 해당 댓글)를 돌려준다. 남의 알림은 없는 것처럼 404 */
    @Transactional
    public String open(Long id, String username) {
        Notification notification = notificationRepository.findById(id)
                .filter(n -> n.getRecipient().getUsername().equals(username))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."));
        notification.markRead();
        return notification.targetUrl();
    }

    @Transactional
    public void markAllRead(String username) {
        notificationRepository.markAllRead(username);
    }
}
