package org.ttt.safevaultbackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分享响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareResponse {

    private String shareId;
    private String shareToken;
    private Long expiresAt;
}
