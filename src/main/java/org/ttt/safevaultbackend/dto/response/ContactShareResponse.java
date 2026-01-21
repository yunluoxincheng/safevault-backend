package org.ttt.safevaultbackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.ttt.safevaultbackend.entity.ContactShareStatus;

/**
 * 联系人分享响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactShareResponse {

    private String shareId;
    private String passwordId;
    private ContactShareStatus status;
    private Long createdAt;
    private Long expiresAt;
}
