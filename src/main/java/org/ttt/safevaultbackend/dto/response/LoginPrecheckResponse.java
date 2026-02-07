package org.ttt.safevaultbackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录预检查响应（Challenge-Response 机制）
 * 返回服务器生成的挑战码（nonce）供客户端签名
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginPrecheckResponse {

    /**
     * 服务器生成的随机挑战码（nonce）
     * 客户端需要用派生密钥对 nonce 进行签名
     */
    private String nonce;

    /**
     * Nonce 过期时间（Unix 时间戳，秒）
     * 默认 30 秒有效期
     */
    private Long expiresAt;

    /**
     * 用户ID（用于前端显示）
     */
    private String userId;
}
