package org.ttt.safevaultbackend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.ttt.safevaultbackend.dto.SharePermission;

/**
 * 创建联系人分享请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateContactShareRequest {

    @NotBlank(message = "接收方用户ID不能为空")
    private String toUserId;

    @NotBlank(message = "密码ID不能为空")
    private String passwordId;

    @NotBlank(message = "标题不能为空")
    private String title;

    private String username;

    @NotBlank(message = "加密的密码不能为空")
    private String encryptedPassword;

    private String url;

    private String notes;

    @Min(value = 1, message = "有效期必须大于0")
    @Builder.Default
    private Integer expiresInMinutes = 1440; // 默认24小时

    @NotNull(message = "权限设置不能为空")
    private SharePermission permission;
}
