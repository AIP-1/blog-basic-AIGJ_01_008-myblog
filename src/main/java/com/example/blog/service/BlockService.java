package com.example.blog.service;

import com.example.blog.domain.User;
import com.example.blog.domain.UserBlock;
import com.example.blog.repository.NotificationRepository;
import com.example.blog.repository.SubscriptionRepository;
import com.example.blog.repository.UserBlockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * 회원 차단. 차단하면
 * - 차단한 사람에게: 상대의 글이 목록에서 빠지고, 상대의 댓글은 접혀 보인다
 * - 차단당한 사람은: 차단한 사람의 글에 댓글, 댓글에 답글, 블로그 구독을 할 수 없다
 * - 서로의 구독은 끊기고, 차단당한 사람이 보냈던 알림은 지워진다
 */
@Service
@Transactional(readOnly = true)
public class BlockService {

    private final UserBlockRepository blockRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final NotificationRepository notificationRepository;
    private final UserService userService;

    public BlockService(UserBlockRepository blockRepository, SubscriptionRepository subscriptionRepository,
                        NotificationRepository notificationRepository, UserService userService) {
        this.blockRepository = blockRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.notificationRepository = notificationRepository;
        this.userService = userService;
    }

    /** blocker 가 blocked 를 차단했는지 (아이디가 null 이면 false) */
    public boolean isBlocked(String blocker, String blocked) {
        return blocker != null && blocked != null
                && blockRepository.existsByBlockerUsernameAndBlockedUsername(blocker, blocked);
    }

    /** 내가 차단한 사람들의 회원 id (비로그인은 빈 집합) */
    public Set<Long> blockedIds(String username) {
        return username == null ? Set.of() : blockRepository.findBlockedIds(username);
    }

    public List<UserBlock> blocks(String username) {
        return blockRepository.findByBlockerUsernameOrderByCreatedAtDesc(username);
    }

    @Transactional
    public void block(String blockerName, String blockedName) {
        if (blockerName.equals(blockedName)) {
            throw new IllegalArgumentException("나 자신은 차단할 수 없습니다.");
        }
        User blocker = userService.get(blockerName);
        User blocked = userService.get(blockedName);
        if (blockRepository.findByBlockerAndBlocked(blocker, blocked).isEmpty()) {
            blockRepository.save(new UserBlock(blocker, blocked));
        }
        subscriptionRepository.findBySubscriberAndBlogOwner(blocker, blocked).ifPresent(subscriptionRepository::delete);
        subscriptionRepository.findBySubscriberAndBlogOwner(blocked, blocker).ifPresent(subscriptionRepository::delete);
        notificationRepository.deleteByRecipientAndActor(blocker, blocked);
    }

    @Transactional
    public void unblock(String blockerName, String blockedName) {
        blockRepository.findByBlockerAndBlocked(userService.get(blockerName), userService.get(blockedName))
                .ifPresent(blockRepository::delete);
    }
}
