package org.ttt.safevaultbackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 密码分享实体
 */
@Entity
@Table(name = "password_shares")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordShare {

    @Id
    @Column(name = "share_id", length = 36)
    private String shareId;

    @Column(name = "password_id", nullable = false)
    private String passwordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_user_id", nullable = false, referencedColumnName = "user_id")
    private User fromUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_user_id", referencedColumnName = "user_id")
    private User toUser;

    @Column(name = "encrypted_data", nullable = false, columnDefinition = "TEXT")
    private String encryptedData;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "can_view", nullable = false)
    @Builder.Default
    private boolean canView = true;

    @Column(name = "can_save", nullable = false)
    @Builder.Default
    private boolean canSave = true;

    @Column(name = "is_revocable", nullable = false)
    @Builder.Default
    private boolean isRevocable = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ShareStatus status = ShareStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShareType shareType;

    @OneToMany(mappedBy = "share", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ShareAuditLog> auditLogs = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
