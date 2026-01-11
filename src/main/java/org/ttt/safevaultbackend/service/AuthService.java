package org.ttt.safevaultbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ttt.safevaultbackend.dto.request.*;
import org.ttt.safevaultbackend.dto.response.AuthResponse;
import org.ttt.safevaultbackend.dto.response.LoginPrecheckResponse;
import org.ttt.safevaultbackend.entity.User;
import org.ttt.safevaultbackend.exception.BusinessException;
import org.ttt.safevaultbackend.exception.ResourceNotFoundException;
import org.ttt.safevaultbackend.repository.UserRepository;
import org.ttt.safevaultbackend.security.JwtTokenProvider;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 认证服务
 * 支持旧的设备 ID 认证和新的零知识邮箱认证
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
     * 通过用户名登录
     */
    @Transactional(readOnly = true)
    public AuthResponse loginByUsername(LoginByUsernameRequest request) {
        // 通过用户名查找用户
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", request.getUsername()));

        // TODO: 验证签名（需要实现签名验证逻辑）
        // 暂时跳过签名验证

        // 生成 Token
        String accessToken = tokenProvider.generateAccessToken(user.getUserId());
        String refreshToken = tokenProvider.generateRefreshToken(user.getUserId());

        return AuthResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
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

    // ========== 零知识架构认证方法 ==========

    /**
     * 零知识架构用户注册
     * 客户端发送主密码验证器，而非主密码本身
     */
    @Transactional
    public AuthResponse registerZeroKnowledge(ZeroKnowledgeRegisterRequest request) {
        // 检查邮箱是否已存在
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("EMAIL_ALREADY_EXISTS", "邮箱已被注册");
        }

        // 检查用户名是否已存在
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("USERNAME_ALREADY_EXISTS", "用户名已存在");
        }

        // 创建新用户（零知识架构）
        User user = User.builder()
                .userId(UUID.randomUUID().toString())
                .username(request.getUsername())
                .displayName(request.getDisplayName())
                .email(request.getEmail())
                // 零知识认证参数
                .passwordVerifier(request.getPasswordVerifier())
                .passwordSalt(request.getPasswordSalt())
                .passwordIterations(request.getPasswordIterations() != null ?
                    request.getPasswordIterations() : 100000)
                // 分享用密钥对
                .publicKey(request.getPublicKey())
                .privateKeyEncrypted(request.getEncryptedPrivateKey())
                .privateKeyIv(request.getPrivateKeyIv())
                // 可选字段
                .keyHash(request.getKeyHash())
                .encryptedMasterKey(request.getEncryptedMasterKey())
                .masterKeyIv(request.getMasterKeyIv())
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
     * 零知识登录第一步：预检查
     * 返回验证器参数供客户端进行密钥派生和验证
     */
    @Transactional(readOnly = true)
    public LoginPrecheckResponse loginPrecheck(LoginPrecheckRequest request) {
        // 查找用户
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        // 检查用户是否已完成零知识设置
        if (user.getPasswordVerifier() == null || user.getPasswordSalt() == null) {
            throw new BusinessException("USER_NOT_SETUP", "用户未完成零知识设置");
        }

        return LoginPrecheckResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .publicKey(user.getPublicKey())
                // 零知识认证参数
                .passwordVerifier(user.getPasswordVerifier())
                .passwordSalt(user.getPasswordSalt())
                .passwordIterations(user.getPasswordIterations())
                // 加密的私钥
                .encryptedPrivateKey(user.getPrivateKeyEncrypted())
                .privateKeyIv(user.getPrivateKeyIv())
                .build();
    }

    /**
     * 零知识登录第二步：验证
     * 客户端计算验证器并发送到服务器进行验证
     */
    @Transactional(readOnly = true)
    public AuthResponse loginVerify(LoginVerifyRequest request) {
        // 查找用户
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        // 验证客户端计算的验证器是否匹配
        if (!user.getPasswordVerifier().equals(request.getClientVerifier())) {
            throw new BusinessException("INVALID_PASSWORD", "主密码不正确");
        }

        // 可选：验证密钥哈希
        if (user.getKeyHash() != null && request.getClientKeyHash() != null) {
            if (!user.getKeyHash().equals(request.getClientKeyHash())) {
                throw new BusinessException("INVALID_KEY_HASH", "密钥哈希不匹配");
            }
        }

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
}
