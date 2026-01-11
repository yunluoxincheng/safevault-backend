package org.ttt.safevaultbackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.ttt.safevaultbackend.dto.request.*;
import org.ttt.safevaultbackend.dto.response.AuthResponse;
import org.ttt.safevaultbackend.dto.response.LoginPrecheckResponse;
import org.ttt.safevaultbackend.service.AuthService;

/**
 * 认证控制器
 * 支持旧的设备 ID 认证和新的零知识邮箱认证
 */
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Tag(name = "认证", description = "用户注册、登录和令牌刷新")
public class AuthController {

    private final AuthService authService;

    // ========== 旧版认证（保留兼容） ==========

    @PostMapping("/register")
    @Operation(summary = "用户注册（旧版）", description = "使用设备 ID 注册新用户")
    @Deprecated
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录（旧版）", description = "使用设备 ID 登录")
    @Deprecated
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login/by-username")
    @Operation(summary = "通过用户名登录", description = "使用用户名登录（支持多账号切换）")
    public ResponseEntity<AuthResponse> loginByUsername(@Valid @RequestBody LoginByUsernameRequest request) {
        AuthResponse response = authService.loginByUsername(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新令牌", description = "使用刷新令牌获取新的访问令牌")
    public ResponseEntity<AuthResponse> refreshToken(@RequestHeader("Authorization") String authorizationHeader) {
        String refreshToken = authorizationHeader.replace("Bearer ", "");
        AuthResponse response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(response);
    }

    // ========== 零知识架构认证 ==========

    @PostMapping("/register-zero-knowledge")
    @Operation(summary = "零知识用户注册", description = "使用邮箱和主密码验证器注册新用户（零知识架构）")
    public ResponseEntity<AuthResponse> registerZeroKnowledge(@Valid @RequestBody ZeroKnowledgeRegisterRequest request) {
        AuthResponse response = authService.registerZeroKnowledge(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login/precheck")
    @Operation(summary = "登录预检查", description = "零知识登录第一步：获取验证器和 Salt")
    public ResponseEntity<LoginPrecheckResponse> loginPrecheck(@Valid @RequestBody LoginPrecheckRequest request) {
        LoginPrecheckResponse response = authService.loginPrecheck(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login/verify")
    @Operation(summary = "登录验证", description = "零知识登录第二步：验证密码并获取令牌")
    public ResponseEntity<AuthResponse> loginVerify(@Valid @RequestBody LoginVerifyRequest request) {
        AuthResponse response = authService.loginVerify(request);
        return ResponseEntity.ok(response);
    }
}
