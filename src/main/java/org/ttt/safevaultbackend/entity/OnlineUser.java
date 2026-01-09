package org.ttt.safevaultbackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 在线用户实体
 */
@Entity
@Table(name = "online_users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnlineUser {

    @Id
    @Column(name = "user_id", length = 36)
    private String userId;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "last_seen", nullable = false)
    private LocalDateTime lastSeen;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @PrePersist
    protected void onCreate() {
        lastSeen = LocalDateTime.now();
    }
}
