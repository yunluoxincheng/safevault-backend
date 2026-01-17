package org.ttt.safevaultbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ttt.safevaultbackend.entity.RevokedToken;
import org.ttt.safevaultbackend.repository.RevokedTokenRepository;
import org.ttt.safevaultbackend.security.JwtTokenProvider;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * 令牌撤销服务
 * 负责管理和验证已撤销的 JWT 令牌
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenRevokeService {

    private final RevokedTokenRepository revokedTokenRepository;
    private final JwtTokenProvider tokenProvider;

    /**
     * 撤销令牌
     *
     * @param token        要撤销的 JWT 令牌
     * @param userId       用户 ID
     * @param deviceId     设备 ID
     * @param revokeReason 撤销原因
     */
    @Transactional
    public void revokeToken(String token, String userId, String deviceId, String revokeReason) {
        try {
            // 计算令牌哈希值
            String tokenHash = hashToken(token);

            // 获取令牌过期时间
            LocalDateTime expiresAt = getTokenExpiration(token);

            // 检查是否已经撤销
            if (revokedTokenRepository.existsByToken(tokenHash)) {
                log.debug("令牌已被撤销: userId={}, deviceId={}", userId, deviceId);
                return;
            }

            // 创建撤销记录
            RevokedToken revokedToken = RevokedToken.builder()
                    .userId(userId)
                    .deviceId(deviceId)
                    .token(tokenHash)
                    .expiresAt(expiresAt)
                    .revokeReason(revokeReason)
                    .build();

            revokedTokenRepository.save(revokedToken);

            log.info("令牌已撤销: userId={}, deviceId={}, reason={}", userId, deviceId, revokeReason);

        } catch (Exception e) {
            log.error("撤销令牌失败: userId={}, deviceId={}", userId, deviceId, e);
            throw new RuntimeException("撤销令牌失败", e);
        }
    }

    /**
     * 检查令牌是否已被撤销
     *
     * @param token    JWT 令牌
     * @param userId   用户 ID
     * @param deviceId 设备 ID
     * @return true 表示令牌已被撤销
     */
    @Transactional(readOnly = true)
    public boolean isTokenRevoked(String token, String userId, String deviceId) {
        try {
            String tokenHash = hashToken(token);
            return revokedTokenRepository.isTokenRevoked(userId, deviceId, tokenHash, LocalDateTime.now());
        } catch (Exception e) {
            log.error("检查令牌撤销状态失败: userId={}", userId, e);
            return false;
        }
    }

    /**
     * 撤销用户的所有令牌（用于密码修改或账户删除）
     *
     * @param userId       用户 ID
     * @param revokeReason 撤销原因
     */
    @Transactional
    public void revokeAllUserTokens(String userId, String revokeReason) {
        try {
            int count = revokedTokenRepository.markAllUserTokens(userId, revokeReason);
            log.info("标记用户所有令牌为已撤销: userId={}, count={}, reason={}", userId, count, revokeReason);
        } catch (Exception e) {
            log.error("撤销用户所有令牌失败: userId={}", userId, e);
            throw new RuntimeException("撤销令牌失败", e);
        }
    }

    /**
     * 定时清理过期的撤销记录
     * 每天凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        try {
            int deleted = revokedTokenRepository.deleteExpiredTokens(LocalDateTime.now());
            log.info("清理过期的撤销记录: count={}", deleted);
        } catch (Exception e) {
            log.error("清理过期撤销记录失败", e);
        }
    }

    /**
     * 计算令牌的 SHA-256 哈希值
     *
     * @param token JWT 令牌
     * @return Base64 编码的哈希值
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("计算令牌哈希失败", e);
        }
    }

    /**
     * 获取令牌的过期时间
     *
     * @param token JWT 令牌
     * @return 过期时间
     */
    private LocalDateTime getTokenExpiration(String token) {
        try {
            // 解析 JWT 获取过期时间
            io.jsonwebtoken.JwtParserBuilder parserBuilder = io.jsonwebtoken.Jwts.parser();

            // 使用与 JwtTokenProvider 相同的签名密钥
            io.jsonwebtoken.Jws<io.jsonwebtoken.Claims> jws =
                parserBuilder
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);

            java.util.Date expiration = jws.getPayload().getExpiration();
            return expiration != null
                ? expiration.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
                : LocalDateTime.now().plusDays(30); // 默认30天后过期

        } catch (Exception e) {
            log.warn("解析令牌过期时间失败，使用默认值: {}", e.getMessage());
            return LocalDateTime.now().plusDays(30);
        }
    }

    /**
     * 获取签名密钥（与 JwtTokenProvider 保持一致）
     * 注意：这里需要从 JwtTokenProvider 获取或注入配置
     */
    private javax.crypto.SecretKey getSigningKey() {
        // 这里简化实现，实际应该从 JwtTokenProvider 获取
        // 由于循环依赖问题，这里使用反射或重新创建密钥
        // 生产环境建议重构为共享密钥生成逻辑
        String jwtSecret = "your-256-bit-secret-key-should-be-at-least-32-characters-long";
        return io.jsonwebtoken.security.Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}
