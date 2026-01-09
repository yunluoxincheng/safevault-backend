package org.ttt.safevaultbackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.ttt.safevaultbackend.dto.PasswordData;
import org.ttt.safevaultbackend.dto.SharePermission;
import org.ttt.safevaultbackend.entity.ShareStatus;
import org.ttt.safevaultbackend.entity.ShareType;

/**
 * 接收分享响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceivedShareResponse {

    private String shareId;
    private String fromUserId;
    private String fromDisplayName;
    private PasswordData passwordData;
    private SharePermission permission;
    private ShareStatus status;
    private ShareType shareType;
    private Long createdAt;
    private Long expiresAt;
}
