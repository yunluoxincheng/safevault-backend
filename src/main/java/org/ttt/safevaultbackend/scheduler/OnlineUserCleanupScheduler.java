package org.ttt.safevaultbackend.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.ttt.safevaultbackend.repository.OnlineUserRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 在线用户清理定时任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OnlineUserCleanupScheduler {

    private final OnlineUserRepository onlineUserRepository;

    /**
     * 每分钟检查一次超时用户（2分钟未活跃）
     */
    @Scheduled(cron = "0 * * * * ?")
    @Transactional
    public void cleanupInactiveUsers() {
        log.debug("Cleaning up inactive online users...");

        // 2分钟前的时间阈值
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(2);
        List<org.ttt.safevaultbackend.entity.OnlineUser> inactiveUsers =
                onlineUserRepository.findByLastSeenBefore(threshold);

        for (org.ttt.safevaultbackend.entity.OnlineUser user : inactiveUsers) {
            onlineUserRepository.delete(user);
            log.debug("Removed inactive online user: userId={}", user.getUserId());
        }

        if (!inactiveUsers.isEmpty()) {
            log.info("Cleaned up {} inactive online users", inactiveUsers.size());
        }
    }
}
