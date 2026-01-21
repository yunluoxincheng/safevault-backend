package org.ttt.safevaultbackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.ttt.safevaultbackend.entity.ContactShareStatus;

/**
 * 发送的联系人分享响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SentContactShareResponse {

    private String shareId;
    private String toUserId;
    private String toDisplayName;
    private String passwordId;
    private String passwordTitle;
    private ContactShareStatus status;
    private Long createdAt;
    private Long expiresAt;
}
