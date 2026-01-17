package org.ttt.safevaultbackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 邮箱注册响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailRegistrationResponse {

    private String message;
    private String email;
    private boolean emailSent;
    private long expiresInSeconds; // 令牌有效期（秒）
}
