package org.ttt.safevaultbackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 附近用户响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NearbyUserResponse {

    private String userId;
    private String username;
    private String displayName;
    private Double distance;
    private Long lastSeen;
}
