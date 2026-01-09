package org.ttt.safevaultbackend.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.ttt.safevaultbackend.entity.ShareStatus;
import org.ttt.safevaultbackend.repository.PasswordShareRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 分享过期检查定时任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShareExpirationScheduler {

    private final PasswordShareRepository shareRepository;

    /**
     * 每小时检查一次过期分享
     */
    @Scheduled(cron = "0 0 * * * ?")
    @Transactional
    public void checkExpiredShares() {
        log.info("Checking for expired shares...");

        LocalDateTime now = LocalDateTime.now();
        List<org.ttt.safevaultbackend.entity.PasswordShare> expiredShares =
                shareRepository.findExpiredShares(now);

        for (org.ttt.safevaultbackend.entity.PasswordShare share : expiredShares) {
            share.setStatus(ShareStatus.EXPIRED);
            shareRepository.save(share);
            log.info("Marked share as expired: shareId={}", share.getShareId());
        }

        if (!expiredShares.isEmpty()) {
            log.info("Expired {} shares", expiredShares.size());
        }
    }
}
