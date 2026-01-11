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
 * 用户实体
 * 零知识架构：主密码验证器存储，不含主密码本身
 */
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @Column(name = "user_id", length = 36)
    private String userId;

    // 设备 ID（保留用于兼容，但改为可选）
    @Column(name = "device_id", unique = true, length = 255)
    private String deviceId;

    // 用户名（保留用于显示和搜索）
    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    // 邮箱（零知识架构的主要登录标识）
    @Column(unique = true, length = 255)
    private String email;

    // 零知识认证相关字段
    @Column(name = "password_verifier", length = 88)
    private String passwordVerifier; // PBKDF2 派生的验证器

    @Column(name = "password_salt", length = 44)
    private String passwordSalt; // Salt 用于密钥派生

    @Column(name = "password_iterations", nullable = false)
    @Builder.Default
    private Integer passwordIterations = 100000; // PBKDF2 迭代次数

    // 加密的密钥材料（用于生物识别和解密）
    @Column(name = "encrypted_master_key", columnDefinition = "TEXT")
    private String encryptedMasterKey; // 加密的主密钥（生物识别用）

    @Column(name = "master_key_iv", length = 24)
    private String masterKeyIv; // 主密钥加密的 IV

    @Column(name = "key_hash", length = 88)
    private String keyHash; // 主密钥哈希（用于快速验证）

    // 分享用密钥对
    @Column(name = "public_key", nullable = false, columnDefinition = "TEXT")
    private String publicKey; // RSA 公钥

    @Column(name = "private_key_encrypted", columnDefinition = "TEXT")
    private String privateKeyEncrypted; // 加密的 RSA 私钥

    @Column(name = "private_key_iv", length = 24)
    private String privateKeyIv; // 私钥加密的 IV

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "fromUser", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PasswordShare> createdShares = new ArrayList<>();

    @OneToMany(mappedBy = "toUser", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PasswordShare> receivedShares = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
