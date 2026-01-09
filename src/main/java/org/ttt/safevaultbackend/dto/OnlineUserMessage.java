package org.ttt.safevaultbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 在线用户消息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnlineUserMessage {

    private String userId;
    private String username;
    private String displayName;
    private Double latitude;
    private Double longitude;
    private Long lastSeen;
}
