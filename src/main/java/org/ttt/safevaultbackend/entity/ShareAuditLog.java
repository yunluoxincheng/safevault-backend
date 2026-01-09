package org.ttt.safevaultbackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 分享审计日志实体
 */
@Entity
@Table(name = "share_audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "share_id", nullable = false, referencedColumnName = "share_id")
    private PasswordShare share;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "action_performed_at", nullable = false)
    private LocalDateTime actionPerformedAt;

    @Column(name = "performed_by", length = 36)
    private String performedBy;

    @PrePersist
    protected void onCreate() {
        actionPerformedAt = LocalDateTime.now();
    }
}
