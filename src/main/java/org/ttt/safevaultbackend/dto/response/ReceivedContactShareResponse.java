package org.ttt.safevaultbackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.ttt.safevaultbackend.dto.PasswordData;
import org.ttt.safevaultbackend.dto.SharePermission;
import org.ttt.safevaultbackend.entity.ContactShareStatus;

/**
 * 接收的联系人分享详情响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceivedContactShareResponse {

    private String shareId;
    private String fromUserId;
    private String fromDisplayName;
    private String passwordId;
    private PasswordData passwordData;
    private SharePermission permission;
    private ContactShareStatus status;
    private Long createdAt;
    private Long expiresAt;
    private Long acceptedAt;
}
