package org.ttt.safevaultbackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 零知识架构登录预检查响应
 * 返回验证器参数供客户端进行密钥派生
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginPrecheckResponse {

    private String userId;
    private String username;
    private String displayName;
    private String publicKey; // RSA 公钥（用于分享）

    // 零知识认证参数
    private String passwordVerifier; // 存储的验证器
    private String passwordSalt; // Salt
    private Integer passwordIterations; // 迭代次数

    private String encryptedPrivateKey; // 加密的私钥
    private String privateKeyIv; // 私钥 IV
}
