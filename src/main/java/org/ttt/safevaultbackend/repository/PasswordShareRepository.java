package org.ttt.safevaultbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.ttt.safevaultbackend.entity.PasswordShare;
import org.ttt.safevaultbackend.entity.ShareStatus;
import org.ttt.safevaultbackend.entity.ShareType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 密码分享数据访问层
 */
@Repository
public interface PasswordShareRepository extends JpaRepository<PasswordShare, String> {

    /**
     * 通过分享ID查找
     */
    Optional<PasswordShare> findByShareId(String shareId);

    /**
     * 查找用户创建的分享
     */
    List<PasswordShare> findByFromUser_UserIdOrderByCreatedAtDesc(String fromUserId);

    /**
     * 查找用户接收的分享
     */
    List<PasswordShare> findByToUser_UserIdOrderByCreatedAtDesc(String toUserId);

    /**
     * 查找过期的分享
     */
    List<PasswordShare> findByStatusAndExpiresAtBefore(ShareStatus status, LocalDateTime expiresAt);

    /**
     * 查找待接收的分享
     */
    List<PasswordShare> findByToUser_UserIdAndStatusOrderByCreatedAtDesc(String toUserId, ShareStatus status);

    /**
     * 查找指定类型的分享
     */
    List<PasswordShare> findByShareTypeAndFromUser_UserIdOrderByCreatedAtDesc(ShareType shareType, String fromUserId);

    /**
     * 查找活跃的分享
     */
    @Query("SELECT ps FROM PasswordShare ps WHERE ps.fromUser.userId = :userId AND ps.status = :status ORDER BY ps.createdAt DESC")
    List<PasswordShare> findActiveSharesByUser(@Param("userId") String userId, @Param("status") ShareStatus status);

    /**
     * 查找所有过期的分享（用于定时任务）
     */
    @Query("SELECT ps FROM PasswordShare ps WHERE ps.status IN ('PENDING', 'ACTIVE', 'ACCEPTED') AND ps.expiresAt < :currentTime")
    List<PasswordShare> findExpiredShares(@Param("currentTime") LocalDateTime currentTime);

    /**
     * 查找所有活跃的云端直接分享
     */
    @Query("SELECT ps FROM PasswordShare ps WHERE ps.shareType = 'DIRECT' AND ps.status = 'ACTIVE' AND ps.expiresAt > :currentTime ORDER BY ps.createdAt DESC")
    List<PasswordShare> findActiveDirectShares(@Param("currentTime") LocalDateTime currentTime);

    /**
     * 撤销用户创建的所有活动分享
     * @param userId 用户ID
     * @return 撤销的分享数量
     */
    @Modifying
    @Query("UPDATE PasswordShare ps SET ps.status = 'REVOKED' WHERE ps.fromUser.userId = :userId AND ps.status IN ('PENDING', 'ACTIVE', 'ACCEPTED')")
    int revokeActiveSharesByFromUser(@Param("userId") String userId);

    /**
     * 删除用户相关的所有分享记录（创建的和接收的）
     * @param userId 用户ID
     * @return 删除的记录数
     */
    @Modifying
    @Query("DELETE FROM PasswordShare ps WHERE ps.fromUser.userId = :userId OR ps.toUser.userId = :userId")
    long deleteAllByFromUserOrToUser(@Param("userId") String userId);
}
