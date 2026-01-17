package org.ttt.safevaultbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ttt.safevaultbackend.entity.User;
import org.ttt.safevaultbackend.exception.BusinessException;
import org.ttt.safevaultbackend.exception.ResourceNotFoundException;
import org.ttt.safevaultbackend.repository.OnlineUserRepository;
import org.ttt.safevaultbackend.repository.PasswordShareRepository;
import org.ttt.safevaultbackend.repository.UserRepository;
import org.ttt.safevaultbackend.repository.UserVaultRepository;

import java.time.LocalDateTime;

/**
 * 账户服务
 * 处理账户删除等账户级操作
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final UserRepository userRepository;
    private final UserVaultRepository userVaultRepository;
    private final PasswordShareRepository passwordShareRepository;
    private final OnlineUserRepository onlineUserRepository;

    /**
     * 删除账户及所有相关数据
     * 使用事务确保操作的原子性
     *
     * @param userId 用户ID
     * @throws ResourceNotFoundException 如果用户不存在
     * @throws BusinessException 如果删除失败
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteAccount(String userId) {
        log.info("开始删除账户: userId={}", userId);

        // 1. 查找用户
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));

        log.info("找到用户: userId={}, email={}", user.getUserId(), user.getEmail());

        // 2. 撤销所有活动的分享（用户创建的）
        int revokedShares = passwordShareRepository.revokeActiveSharesByFromUser(userId);
        log.info("撤销用户创建的活动分享: count={}", revokedShares);

        // 3. 删除所有分享记录（创建的和接收的）
        long deletedShares = passwordShareRepository.deleteAllByFromUserOrToUser(userId);
        log.info("删除用户相关的所有分享记录: count={}", deletedShares);

        // 4. 删除密码库数据
        userVaultRepository.deleteByUserId(userId);
        log.info("删除用户密码库: 成功");

        // 5. 清理在线用户记录
        int cleanedOnlineUsers = onlineUserRepository.deleteByUserId(userId);
        log.info("清理在线用户记录: count={}", cleanedOnlineUsers);

        // 6. 删除用户记录（最后删除，因为其他表有外键引用）
        userRepository.delete(user);

        log.info("账户删除成功: userId={}", userId);
    }
}
