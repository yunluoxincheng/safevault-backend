package org.ttt.safevaultbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ttt.safevaultbackend.dto.request.LoginRequest;
import org.ttt.safevaultbackend.dto.request.RegisterRequest;
import org.ttt.safevaultbackend.dto.response.AuthResponse;
import org.ttt.safevaultbackend.entity.User;
import org.ttt.safevaultbackend.exception.BusinessException;
import org.ttt.safevaultbackend.exception.ResourceNotFoundException;
import org.ttt.safevaultbackend.repository.UserRepository;
import org.ttt.safevaultbackend.security.JwtTokenProvider;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 认证服务
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;

    /**
     * 用户注册
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("USER_ALREADY_EXISTS", "用户名已存在");
        }

        // 检查设备ID是否已存在
        if (userRepository.existsByDeviceId(request.getDeviceId())) {
            throw new BusinessException("DEVICE_ALREADY_EXISTS", "设备ID已存在");
        }

        // 创建新用户
        User user = User.builder()
                .userId(UUID.randomUUID().toString())
                .deviceId(request.getDeviceId())
                .username(request.getUsername())
                .displayName(request.getDisplayName())
                .publicKey(request.getPublicKey())
                .build();

        user = userRepository.save(user);

        // 生成 Token
        String accessToken = tokenProvider.generateAccessToken(user.getUserId());
        String refreshToken = tokenProvider.generateRefreshToken(user.getUserId());

        return AuthResponse.builder()
                .userId(user.getUserId())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(tokenProvider.getAccessTokenExpirationSeconds())
                .build();
    }

    /**
     * 用户登录
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // 查找用户
        User user = userRepository.findByUserId(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", request.getUserId()));

        // 验证设备ID
        if (!user.getDeviceId().equals(request.getDeviceId())) {
            throw new BusinessException("INVALID_DEVICE", "设备ID不匹配");
        }

        // TODO: 验证签名（需要实现签名验证逻辑）
        // 暂时跳过签名验证

        // 生成 Token
        String accessToken = tokenProvider.generateAccessToken(user.getUserId());
        String refreshToken = tokenProvider.generateRefreshToken(user.getUserId());

        return AuthResponse.builder()
                .userId(user.getUserId())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(tokenProvider.getAccessTokenExpirationSeconds())
                .build();
    }

    /**
     * 刷新令牌
     */
    public AuthResponse refreshToken(String refreshToken) {
        // 验证刷新令牌
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new BusinessException("INVALID_REFRESH_TOKEN", "无效的刷新令牌");
        }

        // 获取用户ID
        String userId = tokenProvider.getUserIdFromToken(refreshToken);

        // 验证用户是否存在
        if (!userRepository.existsByUserId(userId)) {
            throw new ResourceNotFoundException("User", "userId", userId);
        }

        // 生成新的访问令牌
        String newAccessToken = tokenProvider.generateAccessToken(userId);
        String newRefreshToken = tokenProvider.generateRefreshToken(userId);

        return AuthResponse.builder()
                .userId(userId)
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(tokenProvider.getAccessTokenExpirationSeconds())
                .build();
    }

    /**
     * 验证令牌
     */
    public boolean validateToken(String token) {
        return tokenProvider.validateToken(token);
    }
}
