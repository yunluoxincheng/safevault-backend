package org.ttt.safevaultbackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.ttt.safevaultbackend.dto.request.UploadPrivateKeyRequest;
import org.ttt.safevaultbackend.dto.request.VaultInitRequest;
import org.ttt.safevaultbackend.dto.request.VaultSyncRequest;
import org.ttt.safevaultbackend.dto.response.PrivateKeyResponse;
import org.ttt.safevaultbackend.dto.response.UploadPrivateKeyResponse;
import org.ttt.safevaultbackend.dto.response.VaultResponse;
import org.ttt.safevaultbackend.dto.response.VaultSyncResponse;
import org.ttt.safevaultbackend.service.PrivateKeyService;
import org.ttt.safevaultbackend.service.VaultService;

/**
 * 密码库控制器
 * 零知识架构：处理加密密码库的存储和同步
 */
@RestController
@RequestMapping("/v1/vault")
@RequiredArgsConstructor
@Tag(name = "密码库", description = "加密密码库的存储和同步（零知识架构）")
public class VaultController {

    private final VaultService vaultService;
    private final PrivateKeyService privateKeyService;

    @GetMapping
    @Operation(summary = "获取密码库", description = "获取用户的加密密码库数据")
    public ResponseEntity<VaultResponse> getVault(
            @Parameter(description = "用户 ID（从 JWT Token 中获取）")
            @RequestHeader("X-User-Id") String userId) {
        VaultResponse response = vaultService.getVault(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/initialize")
    @Operation(summary = "初始化密码库", description = "为新用户创建初始密码库")
    public ResponseEntity<VaultResponse> initializeVault(
            @Parameter(description = "用户 ID（从 JWT Token 中获取）")
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody VaultInitRequest request) {
        VaultResponse response = vaultService.initializeVault(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/sync")
    @Operation(summary = "同步密码库", description = "同步用户的加密密码库，支持冲突检测")
    public ResponseEntity<VaultSyncResponse> syncVault(
            @Parameter(description = "用户 ID（从 JWT Token 中获取）")
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody VaultSyncRequest request) {
        VaultSyncResponse response = vaultService.syncVault(userId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    @Operation(summary = "删除密码库", description = "删除用户的密码库")
    public ResponseEntity<Void> deleteVault(
            @Parameter(description = "用户 ID（从 JWT Token 中获取）")
            @RequestHeader("X-User-Id") String userId) {
        vaultService.deleteVault(userId);
        return ResponseEntity.noContent().build();
    }

    // ========== 私钥管理端点 ==========

    @PostMapping("/private-key")
    @Operation(summary = "上传加密私钥", description = "上传用户的加密私钥到云端，支持版本控制")
    public ResponseEntity<UploadPrivateKeyResponse> uploadPrivateKey(
            @Parameter(description = "用户 ID（从 JWT Token 中获取）")
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody UploadPrivateKeyRequest request) {
        UploadPrivateKeyResponse response = privateKeyService.uploadPrivateKey(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/private-key")
    @Operation(summary = "获取加密私钥", description = "从云端获取用户的加密私钥")
    public ResponseEntity<PrivateKeyResponse> getPrivateKey(
            @Parameter(description = "用户 ID（从 JWT Token 中获取）")
            @RequestHeader("X-User-Id") String userId) {
        PrivateKeyResponse response = privateKeyService.getPrivateKey(userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/private-key")
    @Operation(summary = "删除加密私钥", description = "从云端删除用户的加密私钥")
    public ResponseEntity<Void> deletePrivateKey(
            @Parameter(description = "用户 ID（从 JWT Token 中获取）")
            @RequestHeader("X-User-Id") String userId) {
        privateKeyService.deletePrivateKey(userId);
        return ResponseEntity.noContent().build();
    }
}
