package org.ttt.safevaultbackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.ttt.safevaultbackend.dto.request.CreateShareRequest;
import org.ttt.safevaultbackend.dto.response.ReceivedShareResponse;
import org.ttt.safevaultbackend.dto.response.ShareResponse;
import org.ttt.safevaultbackend.service.ShareService;
import org.ttt.safevaultbackend.service.UserService;

import java.util.List;

/**
 * 分享控制器
 */
@RestController
@RequestMapping("/v1/shares")
@RequiredArgsConstructor
@Tag(name = "分享", description = "密码分享管理")
public class ShareController {

    private final ShareService shareService;
    private final UserService userService;

    @PostMapping
    @Operation(summary = "创建分享", description = "创建密码分享（支持三种类型：直接分享、用户对用户、附近设备）")
    public ResponseEntity<ShareResponse> createShare(@Valid @RequestBody CreateShareRequest request) {
        String userId = userService.getCurrentUserId();
        ShareResponse response = shareService.createShare(request, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{shareId}")
    @Operation(summary = "接收分享", description = "通过分享ID获取分享的密码详情")
    public ResponseEntity<ReceivedShareResponse> receiveShare(@PathVariable String shareId) {
        String userId = userService.getCurrentUserId();
        ReceivedShareResponse response = shareService.receiveShare(shareId, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{shareId}/revoke")
    @Operation(summary = "撤销分享", description = "撤销指定的密码分享")
    public ResponseEntity<Void> revokeShare(@PathVariable String shareId) {
        String userId = userService.getCurrentUserId();
        shareService.revokeShare(shareId, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{shareId}/save")
    @Operation(summary = "保存分享", description = "保存接收到的分享密码")
    public ResponseEntity<Void> saveSharedPassword(@PathVariable String shareId) {
        String userId = userService.getCurrentUserId();
        shareService.saveSharedPassword(shareId, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/created")
    @Operation(summary = "获取创建的分享", description = "获取当前用户创建的所有分享")
    public ResponseEntity<List<ReceivedShareResponse>> getMyShares() {
        String userId = userService.getCurrentUserId();
        List<ReceivedShareResponse> response = shareService.getMyShares(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/received")
    @Operation(summary = "获取接收的分享", description = "获取当前用户接收的所有分享")
    public ResponseEntity<List<ReceivedShareResponse>> getReceivedShares() {
        String userId = userService.getCurrentUserId();
        List<ReceivedShareResponse> response = shareService.getReceivedShares(userId);
        return ResponseEntity.ok(response);
    }
}
