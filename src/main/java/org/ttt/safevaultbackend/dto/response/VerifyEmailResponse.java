package org.ttt.safevaultbackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 邮箱验证响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyEmailResponse {

    private boolean success;
    private String message;
    private String email;
    private String username;
}
