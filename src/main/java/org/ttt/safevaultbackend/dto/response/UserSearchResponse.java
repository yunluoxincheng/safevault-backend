package org.ttt.safevaultbackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户搜索响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchResponse {

    private String userId;
    private String username;
    private String displayName;
    private String publicKey;
}
