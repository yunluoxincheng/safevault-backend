package org.ttt.safevaultbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ttt.safevaultbackend.dto.request.*;
import org.ttt.safevaultbackend.dto.response.AuthResponse;
import org.ttt.safevaultbackend.dto.response.CompleteRegistrationResponse;
import org.ttt.safevaultbackend.dto.response.EmailLoginResponse;
import org.ttt.safevaultbackend.dto.response.EmailRegistrationResponse;
import org.ttt.safevaultbackend.dto.response.LogoutResponse;
import org.ttt.safevaultbackend.dto.response.VerifyEmailResponse;
import org.ttt.safevaultbackend.dto.response.VerificationStatusResponse;
import org.ttt.safevaultbackend.dto.DeviceInfo;
import org.ttt.safevaultbackend.dto.PendingUser;
import org.ttt.safevaultbackend.entity.User;
import org.ttt.safevaultbackend.exception.BusinessException;
import org.ttt.safevaultbackend.exception.ResourceNotFoundException;
import org.ttt.safevaultbackend.repository.UserRepository;
import org.ttt.safevaultbackend.security.JwtTokenProvider;
import org.ttt.safevaultbackend.service.EmailService;
import org.ttt.safevaultbackend.service.PendingUserService;
import org.ttt.safevaultbackend.service.VerificationTokenService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 认证服务
 * 支持旧的设备 ID 认证和新的邮箱认证
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;
    private final EmailService emailService;
    private final VerificationTokenService verificationTokenService;
    private final PendingUserService pendingUserService;
    private final CryptoService cryptoService;
    private final TokenRevokeService tokenRevokeService;
    private final VerificationEventService verificationEventService;
    private final EmailVerificationHistoryService verificationHistoryService;

    @Value("${email.verification.token-expiration-minutes:10}")
    private int tokenExpirationMinutes;

    @Value("${app.base-url:http://localhost:8080/api}")
    private String baseUrl;

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

        // 验证签名（验证时间戳签名，防止重放攻击）
        verifyLoginSignature(request.getUserId(), request.getDeviceId(), request.getSignature(), request.getTimestamp());

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

        // 验证签名（验证用户名+设备ID的时间戳签名，防止重放攻击）
        verifyUsernameLoginSignature(request.getUsername(), user.getDeviceId(), request.getSignature(), request.getTimestamp());

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

    // ========== 统一邮箱认证方法 ==========

    /**
     * 邮箱注册第一步：发起注册并发送验证邮件
     * 数据先存入 Redis，验证成功后才写入数据库
     *
     * @param request 邮箱和用户名
     * @return 注册响应
     */
    public EmailRegistrationResponse registerWithEmail(EmailRegistrationRequest request) {
        // 检查邮箱是否已在数据库中注册
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("EMAIL_ALREADY_EXISTS", "该邮箱已被注册");
        }

        // 检查用户名是否已被使用
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("USERNAME_ALREADY_EXISTS", "该用户名已被使用");
        }

        // 检查邮箱是否已有待验证记录
        PendingUser existingPendingUser = pendingUserService.getPendingUserByEmail(request.getEmail());
        if (existingPendingUser != null) {
            // 检查是否过期
            if (existingPendingUser.isTokenExpired()) {
                // 过期：自动清理并继续注册流程
                log.info("待验证记录已过期，自动清理: email={}", request.getEmail());
                pendingUserService.deletePendingUser(request.getEmail());
            } else {
                // 未过期：检查用户名是否一致
                if (!existingPendingUser.getUsername().equals(request.getUsername())) {
                    throw new BusinessException("USERNAME_MISMATCH",
                            "该邮箱已有待验证的注册申请（用户名: " + existingPendingUser.getUsername() + "），请使用相同用户名或等待过期");
                }

                // 检查是否可以重发（60秒冷却）
                if (!existingPendingUser.canResendEmail()) {
                    long waitSeconds = 60 - java.time.Duration.between(
                            existingPendingUser.getLastEmailSentAt(),
                            java.time.LocalDateTime.now()
                    ).getSeconds();
                    throw new BusinessException("RESEND_TOO_SOON",
                            "验证邮件已发送，请查收。如需重发，请等待 " + waitSeconds + " 秒后重试");
                }

                // 自动重发验证邮件
                log.info("检测到已有待验证记录，自动重发邮件: email={}", request.getEmail());
                return resendVerificationEmail(new ResendVerificationRequest(request.getEmail()));
            }
        }

        // 生成验证令牌
        String token = pendingUserService.generateVerificationToken();

        // 计算过期时间
        LocalDateTime tokenExpiresAt = LocalDateTime.now().plusMinutes(tokenExpirationMinutes);

        // 创建待验证用户（存入 Redis）
        PendingUser pendingUser = PendingUser.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                .displayName(request.getUsername())
                .verificationToken(token)
                .tokenExpiresAt(tokenExpiresAt)
                .createdAt(LocalDateTime.now())
                .lastEmailSentAt(LocalDateTime.now())
                .build();

        // 保存到 Redis
        pendingUserService.savePendingUser(pendingUser);

        // 记录令牌生成事件
        verificationEventService.recordTokenGenerated(null, request.getEmail(), token, null);

        // 记录验证历史（异步）
        verificationHistoryService.recordRegistrationHistory(
            null, request.getEmail(), token, null, tokenExpiresAt, "aliyun"
        );

        // 构建验证链接（Deep Link）
        String verificationUrl = "safevault://verify-email?token=" + token;

        // 发送验证邮件
        boolean emailSent = emailService.sendVerificationEmail(request.getEmail(), verificationUrl);

        // 记录邮件发送事件
        verificationEventService.recordEmailSent(null, request.getEmail(), token, emailSent, emailSent ? null : "邮件发送失败");

        log.info("发起邮箱注册: email={}, username={}, token={}", request.getEmail(), request.getUsername(), token);

        return EmailRegistrationResponse.builder()
                .message("注册成功，请查收验证邮件")
                .email(request.getEmail())
                .emailSent(emailSent)
                .expiresInSeconds((long) tokenExpirationMinutes * 60)
                .build();
    }

    /**
     * 验证邮箱
     * 从 Redis 获取待验证用户，验证成功后写入数据库
     *
     * @param request 验证令牌
     * @return 验证响应
     */
    @Transactional
    public VerifyEmailResponse verifyEmail(VerifyEmailRequest request) {
        try {
            log.info("收到邮箱验证请求: token={}", request.getToken());

            // 从 Redis 获取待验证用户
            PendingUser pendingUser = pendingUserService.getPendingUserByToken(request.getToken());

            if (pendingUser == null) {
                log.warn("验证失败: 未找到待验证用户, token={}", request.getToken());
                return VerifyEmailResponse.builder()
                        .success(false)
                        .message("验证令牌无效或已过期")
                        .build();
            }

            log.info("找到待验证用户: email={}, username={}, tokenExpiresAt={}, createdAt={}",
                    pendingUser.getEmail(),
                    pendingUser.getUsername(),
                    pendingUser.getTokenExpiresAt(),
                    pendingUser.getCreatedAt());

            // 检查令牌是否过期
            if (pendingUser.isTokenExpired()) {
                log.warn("验证失败: 令牌已过期, email={}, tokenExpiresAt={}, now={}",
                        pendingUser.getEmail(),
                        pendingUser.getTokenExpiresAt(),
                        LocalDateTime.now());
                // 清理过期的待验证用户
                pendingUserService.deletePendingUser(pendingUser.getEmail());
                return VerifyEmailResponse.builder()
                        .success(false)
                        .message("验证令牌已过期，请重新注册")
                        .build();
            }

            // 再次检查数据库中是否已存在该邮箱（防止并发注册）
            if (userRepository.existsByEmail(pendingUser.getEmail())) {
                log.warn("验证失败: 该邮箱已被注册, email={}", pendingUser.getEmail());
                // 清理 Redis 中的数据
                pendingUserService.deletePendingUser(pendingUser.getEmail());
                return VerifyEmailResponse.builder()
                        .success(false)
                        .message("该邮箱已被注册")
                        .build();
            }

            // 再次检查用户名是否已存在
            if (userRepository.existsByUsername(pendingUser.getUsername())) {
                log.warn("验证失败: 该用户名已被使用, username={}", pendingUser.getUsername());
                // 清理 Redis 中的数据
                pendingUserService.deletePendingUser(pendingUser.getEmail());
                return VerifyEmailResponse.builder()
                        .success(false)
                        .message("该用户名已被使用")
                        .build();
            }

            // 创建用户记录（邮箱已验证状态）
            User user = User.builder()
                    .userId(UUID.randomUUID().toString())
                    .email(pendingUser.getEmail())
                    .username(pendingUser.getUsername())
                    .displayName(pendingUser.getDisplayName())
                    .emailVerified(true)
                    .build();

            user = userRepository.save(user);

            // 清除 Redis 中的待验证用户
            pendingUserService.deletePendingUser(pendingUser.getEmail());

            // 记录验证成功事件
            verificationEventService.recordTokenVerified(user.getUserId(), pendingUser.getEmail(), request.getToken(), null);

            // 标记验证历史为已使用
            verificationHistoryService.markAsUsed(request.getToken());

            log.info("邮箱验证成功，创建用户: email={}, username={}, userId={}",
                    user.getEmail(), user.getUsername(), user.getUserId());

            return VerifyEmailResponse.builder()
                    .success(true)
                    .message("邮箱验证成功，请设置主密码")
                    .email(user.getEmail())
                    .username(user.getUsername())
                    .build();

        } catch (Exception e) {
            log.error("邮箱验证失败: token={}", request.getToken(), e);
            return VerifyEmailResponse.builder()
                    .success(false)
                    .message("验证失败，请重试")
                    .build();
        }
    }

    /**
     * 重新发送验证邮件
     * 更新 Redis 中的待验证用户记录
     *
     * @param request 邮箱
     * @return 注册响应
     */
    public EmailRegistrationResponse resendVerificationEmail(ResendVerificationRequest request) {
        // 从 Redis 获取待验证用户
        PendingUser pendingUser = pendingUserService.getPendingUserByEmail(request.getEmail());

        if (pendingUser == null) {
            throw new BusinessException("PENDING_USER_NOT_FOUND", "该邮箱没有待验证的注册申请，请先注册");
        }

        // 检查令牌是否过期
        if (pendingUser.isTokenExpired()) {
            log.info("重发验证邮件时发现令牌已过期，自动更新: email={}", request.getEmail());
            // 令牌已过期，直接更新，不检查频率限制
        } else {
            // 频率限制：60秒内只能请求一次
            if (!pendingUser.canResendEmail()) {
                long waitSeconds = 60 - java.time.Duration.between(
                        pendingUser.getLastEmailSentAt(),
                        java.time.LocalDateTime.now()
                ).getSeconds();
                throw new BusinessException("RESEND_TOO_SOON", "请等待 " + waitSeconds + " 秒后再试");
            }
        }

        // 生成新的验证令牌
        String newToken = pendingUserService.generateVerificationToken();
        LocalDateTime tokenExpiresAt = LocalDateTime.now().plusMinutes(tokenExpirationMinutes);

        // 更新 Redis 中的待验证用户
        pendingUserService.updateVerificationToken(request.getEmail(), newToken, tokenExpiresAt);

        // 记录新令牌生成事件
        verificationEventService.recordTokenGenerated(null, request.getEmail(), newToken, null);

        // 记录重发验证历史
        verificationHistoryService.recordRegistrationHistory(
            null, request.getEmail(), newToken, null, tokenExpiresAt, "aliyun"
        );

        // 构建验证链接
        String verificationUrl = "safevault://verify-email?token=" + newToken;

        // 发送验证邮件
        boolean emailSent = emailService.sendVerificationEmail(request.getEmail(), verificationUrl);

        // 记录邮件发送事件
        verificationEventService.recordEmailSent(null, request.getEmail(), newToken, emailSent, emailSent ? null : "邮件发送失败");

        log.info("重新发送验证邮件: email={}, newToken={}", request.getEmail(), newToken);

        return EmailRegistrationResponse.builder()
                .message("验证邮件已重新发送")
                .email(request.getEmail())
                .emailSent(emailSent)
                .expiresInSeconds((long) tokenExpirationMinutes * 60)
                .build();
    }

    /**
     * 检查邮箱验证状态
     * 用于前端轮询检查用户是否已在 Web 页面完成验证
     *
     * @param email 邮箱地址
     * @return 验证状态响应
     */
    @Transactional(readOnly = true)
    public VerificationStatusResponse checkVerificationStatus(String email) {
        // 首先检查数据库中是否已有该邮箱的用户（表示已验证）
        if (userRepository.existsByEmail(email)) {
            User user = userRepository.findByEmail(email).orElse(null);
            if (user != null && user.getEmailVerified()) {
                return VerificationStatusResponse.builder()
                        .status("VERIFIED")
                        .email(user.getEmail())
                        .username(user.getUsername())
                        .build();
            }
        }

        // 检查 Redis 中是否有待验证用户
        PendingUser pendingUser = pendingUserService.getPendingUserByEmail(email);
        if (pendingUser != null) {
            // 检查是否过期
            if (pendingUser.isTokenExpired()) {
                // 过期，清理并返回 NOT_FOUND
                pendingUserService.deletePendingUser(email);
                return VerificationStatusResponse.builder()
                        .status("NOT_FOUND")
                        .email(email)
                        .build();
            }

            // 待验证
            return VerificationStatusResponse.builder()
                    .status("PENDING")
                    .email(pendingUser.getEmail())
                    .username(pendingUser.getUsername())
                    .tokenExpiresAt(pendingUser.getTokenExpiresAt().toString())
                    .build();
        }

        // 无记录
        return VerificationStatusResponse.builder()
                .status("NOT_FOUND")
                .email(email)
                .build();
    }

    // ========== 统一邮箱认证方法 ==========

    /**
     * 邮箱登录（支持设备管理）
     *
     * @param request 邮箱登录请求
     * @return 邮箱登录响应
     */
    @Transactional
    public EmailLoginResponse loginByEmail(LoginByEmailRequest request) {
        // 查找用户
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        // 检查邮箱是否已验证
        if (!user.getEmailVerified()) {
            throw new BusinessException("EMAIL_NOT_VERIFIED", "请先验证邮箱后再登录");
        }

        // 验证派生密钥签名（使用时间戳验证，防止重放攻击）
        verifyDerivedKeySignature(request.getEmail(), request.getDeviceId(), request.getDerivedKeySignature(), request.getTimestamp());

        // 获取当前设备列表
        List<DeviceInfo> devices = getDevicesList(user);

        // 检查是否为新设备
        boolean isNewDevice = devices.stream()
                .noneMatch(d -> d.getDeviceId().equals(request.getDeviceId()));

        if (isNewDevice) {
            // 新设备：添加到设备列表
            DeviceInfo newDevice = DeviceInfo.builder()
                    .deviceId(request.getDeviceId())
                    .deviceName(request.getDeviceName())
                    .deviceType(request.getDeviceType() != null ? request.getDeviceType() : "unknown")
                    .osVersion(request.getOsVersion())
                    .createdAt(LocalDateTime.now())
                    .lastActiveAt(LocalDateTime.now())
                    .isCurrentDevice(true)
                    .build();

            devices.add(newDevice);

            // 更新用户记录
            user.setDevices(DeviceInfo.toJson(devices));
            userRepository.save(user);

            // 生成 Token
            String accessToken = tokenProvider.generateAccessToken(user.getUserId());
            String refreshToken = tokenProvider.generateRefreshToken(user.getUserId());

            return EmailLoginResponse.builder()
                    .userId(user.getUserId())
                    .email(user.getEmail())
                    .username(user.getUsername())
                    .displayName(user.getDisplayName())
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .expiresIn(tokenProvider.getAccessTokenExpirationSeconds())
                    .emailVerified(user.getEmailVerified())
                    .devices(devices)
                    .isNewDevice(true)
                    .message("新设备登录成功")
                    .build();

        } else {
            // 已注册设备：更新最后活跃时间
            devices = devices.stream()
                    .map(d -> {
                        if (d.getDeviceId().equals(request.getDeviceId())) {
                            d.setLastActiveAt(LocalDateTime.now());
                            d.setCurrentDevice(true);
                        } else {
                            d.setCurrentDevice(false);
                        }
                        return d;
                    })
                    .collect(Collectors.toList());

            // 更新用户记录
            user.setDevices(DeviceInfo.toJson(devices));
            userRepository.save(user);

            // 生成 Token
            String accessToken = tokenProvider.generateAccessToken(user.getUserId());
            String refreshToken = tokenProvider.generateRefreshToken(user.getUserId());

            return EmailLoginResponse.builder()
                    .userId(user.getUserId())
                    .email(user.getEmail())
                    .username(user.getUsername())
                    .displayName(user.getDisplayName())
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .expiresIn(tokenProvider.getAccessTokenExpirationSeconds())
                    .emailVerified(user.getEmailVerified())
                    .devices(devices)
                    .isNewDevice(false)
                    .build();
        }
    }

    /**
     * 获取用户的设备列表
     *
     * @param user 用户
     * @return 设备列表
     */
    private List<DeviceInfo> getDevicesList(User user) {
        if (user.getDevices() == null || user.getDevices().isEmpty()) {
            return new ArrayList<>();
        }
        return DeviceInfo.fromJson(user.getDevices());
    }

    // ========== 签名验证方法 ==========

    /**
     * 验证设备ID登录的签名
     * 签名格式：Base64(SHA256(userId + deviceId + timestamp))
     *
     * @param userId    用户ID
     * @param deviceId  设备ID
     * @param signature Base64编码的签名
     * @param timestamp 时间戳（毫秒）
     * @throws BusinessException 验证失败时抛出异常
     */
    private void verifyLoginSignature(String userId, String deviceId, String signature, Long timestamp) {
        // 时间戳验证：允许5分钟的时间窗口，防止重放攻击
        long currentTime = System.currentTimeMillis();
        long timeDiff = timestamp != null ? Math.abs(currentTime - timestamp) : Long.MAX_VALUE;

        // 允许5分钟（300000毫秒）的时间差
        if (timeDiff > 300000) {
            throw new BusinessException("INVALID_TIMESTAMP", "请求时间戳无效或已过期");
        }

        // 如果提供了签名，进行签名验证
        if (signature != null && !signature.isEmpty()) {
            String expectedSignature = computeHashSignature(userId + deviceId + (timestamp != null ? timestamp : currentTime));

            if (!expectedSignature.equals(signature)) {
                throw new BusinessException("INVALID_SIGNATURE", "签名验证失败");
            }
        }
        // 如果未提供签名，允许通过（向后兼容）
    }

    /**
     * 验证用户名登录的签名
     * 签名格式：Base64(SHA256(username + deviceId + timestamp))
     *
     * @param username  用户名
     * @param deviceId  设备ID
     * @param signature Base64编码的签名
     * @param timestamp 时间戳（毫秒）
     * @throws BusinessException 验证失败时抛出异常
     */
    private void verifyUsernameLoginSignature(String username, String deviceId, String signature, Long timestamp) {
        // 时间戳验证：允许5分钟的时间窗口，防止重放攻击
        long currentTime = System.currentTimeMillis();
        long timeDiff = timestamp != null ? Math.abs(currentTime - timestamp) : Long.MAX_VALUE;

        // 允许5分钟（300000毫秒）的时间差
        if (timeDiff > 300000) {
            throw new BusinessException("INVALID_TIMESTAMP", "请求时间戳无效或已过期");
        }

        // 如果提供了签名，进行签名验证
        if (signature != null && !signature.isEmpty()) {
            String expectedSignature = computeHashSignature(username + deviceId + (timestamp != null ? timestamp : currentTime));

            if (!expectedSignature.equals(signature)) {
                throw new BusinessException("INVALID_SIGNATURE", "签名验证失败");
            }
        }
        // 如果未提供签名，允许通过（向后兼容）
    }

    /**
     * 验证邮箱登录的派生密钥签名
     * 签名格式：Base64(HMAC-SHA256(email + deviceId + timestamp, derivedKey))
     *
     * 注意：当前实现简化了签名验证，仅验证时间戳
     * 完整实现需要客户端使用派生密钥生成HMAC签名，服务器使用存储的派生密钥验证
     *
     * @param email    邮箱
     * @param deviceId 设备ID
     * @param signature Base64编码的HMAC签名
     * @param timestamp 时间戳（毫秒）
     * @throws BusinessException 验证失败时抛出异常
     */
    private void verifyDerivedKeySignature(String email, String deviceId, String signature, Long timestamp) {
        // 时间戳验证：允许5分钟的时间窗口，防止重放攻击
        long currentTime = System.currentTimeMillis();
        long timeDiff = timestamp != null ? Math.abs(currentTime - timestamp) : Long.MAX_VALUE;

        // 允许5分钟（300000毫秒）的时间差
        if (timeDiff > 300000) {
            throw new BusinessException("INVALID_TIMESTAMP", "请求时间戳无效或已过期");
        }

        // 当前实现：仅验证签名不为空，实际HMAC验证需要存储派生密钥
        // 生产环境应实现完整的HMAC验证：
        // 1. 从数据库获取用户存储的派生密钥相关数据
        // 2. 使用相同的派生密钥计算HMAC
        // 3. 比较计算结果与提供的签名

        if (signature == null || signature.isEmpty()) {
            throw new BusinessException("INVALID_SIGNATURE", "派生密钥签名不能为空");
        }

        // 简化验证：检查签名格式（Base64编码）
        try {
            java.util.Base64.getDecoder().decode(signature);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("INVALID_SIGNATURE", "签名格式无效");
        }
    }

    /**
     * 计算SHA-256哈希签名（与前端保持一致）
     *
     * @param data 待签名数据
     * @return Base64编码的签名
     */
    private String computeHashSignature(String data) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new BusinessException("SIGNATURE_ERROR", "签名计算失败");
        }
    }

    // ========== 设备管理方法 ==========

    /**
     * 注销登录
     * 清除服务器端会话并撤销令牌
     *
     * @param userId   用户 ID
     * @param token    JWT 令牌（可选，用于撤销）
     * @param request  注销请求
     * @return 注销响应
     */
    @Transactional
    public LogoutResponse logout(String userId, String token, LogoutRequest request) {
        // 验证用户存在
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));

        // 撤销令牌（如果提供）
        if (token != null && !token.isEmpty()) {
            try {
                tokenRevokeService.revokeToken(
                    token,
                    userId,
                    request.getDeviceId(),
                    "LOGOUT"
                );
            } catch (Exception e) {
                log.warn("撤销令牌失败，但继续注销流程: userId={}, deviceId={}", userId, request.getDeviceId(), e);
            }
        }

        log.info("用户注销: userId={}, deviceId={}", userId, request.getDeviceId());

        return LogoutResponse.builder()
                .success(true)
                .message("注销成功")
                .build();
    }

    /**
     * 完成注册
     * 邮箱验证后设置主密码并完成注册
     *
     * @param request 完成注册请求
     * @return 完成注册响应
     */
    @Transactional
    public CompleteRegistrationResponse completeRegistration(CompleteRegistrationRequest request) {
        // 查找用户（通过邮箱）
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        // 验证邮箱已验证
        if (!user.getEmailVerified()) {
            throw new BusinessException("EMAIL_NOT_VERIFIED", "请先验证邮箱");
        }

        // 验证用户名匹配
        if (!user.getUsername().equals(request.getUsername())) {
            throw new BusinessException("USERNAME_MISMATCH", "用户名不匹配");
        }

        // 检查是否已完成注册
        if (user.getPasswordVerifier() != null && !user.getPasswordVerifier().isEmpty()) {
            throw new BusinessException("REGISTRATION_ALREADY_COMPLETED", "注册已完成，请直接登录");
        }

        // 保存密码验证器和盐值
        user.setPasswordVerifier(request.getPasswordVerifier());
        user.setPasswordSalt(request.getSalt());

        // 保存公钥和加密的私钥
        user.setPublicKey(request.getPublicKey());
        user.setPrivateKeyEncrypted(request.getEncryptedPrivateKey());
        user.setPrivateKeyIv(request.getPrivateKeyIv());

        // 设置设备ID（如果是新用户）
        if (user.getDeviceId() == null || user.getDeviceId().isEmpty()) {
            user.setDeviceId(request.getDeviceId());
        }

        user = userRepository.save(user);

        // 生成访问令牌和刷新令牌
        String accessToken = tokenProvider.generateAccessToken(user.getUserId());
        String refreshToken = tokenProvider.generateRefreshToken(user.getUserId());

        log.info("用户完成注册: userId={}, email={}, username={}", user.getUserId(), user.getEmail(), user.getUsername());

        return CompleteRegistrationResponse.builder()
                .success(true)
                .message("注册成功")
                .userId(user.getUserId())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .displayName(user.getDisplayName())
                .build();
    }

    /**
     * 获取用户设备列表
     *
     * @param userId 用户 ID
     * @return 设备列表
     */
    @Transactional(readOnly = true)
    public java.util.List<DeviceInfo> getUserDevices(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));

        if (user.getDevices() == null || user.getDevices().isEmpty()) {
            return new java.util.ArrayList<>();
        }

        return DeviceInfo.fromJson(user.getDevices());
    }

    /**
     * 移除设备
     *
     * @param userId           用户 ID
     * @param deviceIdToRemove 要移除的设备 ID
     * @return 是否成功移除
     */
    @Transactional
    public boolean removeDevice(String userId, String deviceIdToRemove) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));

        java.util.List<DeviceInfo> devices = getUserDevices(userId);

        // 检查是否尝试移除当前设备
        DeviceInfo currentDevice = devices.stream()
                .filter(DeviceInfo::isCurrentDevice)
                .findFirst()
                .orElse(null);

        if (currentDevice != null && currentDevice.getDeviceId().equals(deviceIdToRemove)) {
            throw new BusinessException("CANNOT_REMOVE_CURRENT_DEVICE",
                    "无法移除当前正在使用的设备");
        }

        // 移除设备
        boolean removed = devices.removeIf(d -> d.getDeviceId().equals(deviceIdToRemove));

        if (removed) {
            user.setDevices(DeviceInfo.toJson(devices));
            userRepository.save(user);
            log.info("移除设备: userId={}, deviceId={}", userId, deviceIdToRemove);
        }

        return removed;
    }

    // ========== 调试方法 ==========

    /**
     * 调试方法：根据邮箱获取待验证用户
     */
    public java.util.Map<String, Object> debugGetPendingUserByEmail(String email) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        try {
            org.ttt.safevaultbackend.dto.PendingUser pendingUser =
                    pendingUserService.getPendingUserByEmail(email);

            if (pendingUser != null) {
                result.put("found", true);
                result.put("email", pendingUser.getEmail());
                result.put("username", pendingUser.getUsername());
                result.put("token", pendingUser.getVerificationToken());
                result.put("tokenExpiresAt", pendingUser.getTokenExpiresAt().toString());
                result.put("createdAt", pendingUser.getCreatedAt().toString());
                result.put("isExpired", pendingUser.isTokenExpired());
                result.put("canResend", pendingUser.canResendEmail());
            } else {
                result.put("found", false);
                result.put("message", "未找到待验证用户");
            }
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        return result;
    }

    /**
     * 调试方法：根据token获取待验证用户
     */
    public java.util.Map<String, Object> debugGetPendingUserByToken(String token) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        try {
            org.ttt.safevaultbackend.dto.PendingUser pendingUser =
                    pendingUserService.getPendingUserByToken(token);

            if (pendingUser != null) {
                result.put("found", true);
                result.put("email", pendingUser.getEmail());
                result.put("username", pendingUser.getUsername());
                result.put("token", pendingUser.getVerificationToken());
                result.put("tokenExpiresAt", pendingUser.getTokenExpiresAt().toString());
                result.put("createdAt", pendingUser.getCreatedAt().toString());
                result.put("isExpired", pendingUser.isTokenExpired());
                result.put("canResend", pendingUser.canResendEmail());
            } else {
                result.put("found", false);
                result.put("message", "未找到待验证用户");
            }
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        return result;
    }

    /**
     * 调试方法：获取 Redis 原始数据
     */
    public java.util.Map<String, Object> debugGetRedisRawValue(String email) {
        return pendingUserService.debugGetRawValue(email);
    }
}
