package org.ttt.safevaultbackend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 位置注册请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterLocationRequest {

    @NotNull(message = "纬度不能为空")
    private Double latitude;

    @NotNull(message = "经度不能为空")
    private Double longitude;
}
