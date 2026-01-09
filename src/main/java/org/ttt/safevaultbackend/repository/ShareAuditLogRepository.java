package org.ttt.safevaultbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.ttt.safevaultbackend.entity.ShareAuditLog;

import java.util.List;

/**
 * 分享审计日志数据访问层
 */
@Repository
public interface ShareAuditLogRepository extends JpaRepository<ShareAuditLog, Long> {

    /**
     * 查找指定分享的所有审计日志
     */
    List<ShareAuditLog> findByShare_ShareIdOrderByActionPerformedAtDesc(String shareId);

    /**
     * 查找指定操作类型的审计日志
     */
    List<ShareAuditLog> findByActionOrderByActionPerformedAtDesc(String action);

    /**
     * 查找指定用户执行的审计日志
     */
    List<ShareAuditLog> findByPerformedByOrderByActionPerformedAtDesc(String performedBy);
}
