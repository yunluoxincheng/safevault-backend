package org.ttt.safevaultbackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.ttt.safevaultbackend.dto.request.LoginRequest;
import org.ttt.safevaultbackend.dto.request.RegisterRequest;
import org.ttt.safevaultbackend.dto.response.AuthResponse;
import org.ttt.safevaultbackend.service.AuthService;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Tag(name = "认证", description = "用户注册、登录和令牌刷新")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "注册新用户并返回访问令牌")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户登录并返回访问令牌")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新令牌", description = "使用刷新令牌获取新的访问令牌")
    public ResponseEntity<AuthResponse> refreshToken(@RequestHeader("Authorization") String authorizationHeader) {
        String refreshToken = authorizationHeader.replace("Bearer ", "");
        AuthResponse response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(response);
    }
}
