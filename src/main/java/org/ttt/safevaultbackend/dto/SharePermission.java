package org.ttt.safevaultbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分享权限
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharePermission {

    @Builder.Default
    private boolean canView = true;

    @Builder.Default
    private boolean canSave = true;

    @Builder.Default
    private boolean isRevocable = true;
}
