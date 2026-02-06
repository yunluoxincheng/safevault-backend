package org.ttt.safevaultbackend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT Token 提供者
 */
@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);
    private static final int MIN_SECRET_LENGTH = 32;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    /**
     * 启动时验证 JWT 密钥配置
     * 安全加固：确保密钥长度符合安全要求
     */
    @PostConstruct
    public void validateJwtSecret() {
        if (jwtSecret == null || jwtSecret.isEmpty()) {
            String errorMsg = "JWT_SECRET 环境变量未设置！应用无法启动。";
            logger.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        if (jwtSecret.length() < MIN_SECRET_LENGTH) {
            String errorMsg = String.format(
                "JWT_SECRET 密钥长度不足！当前长度: %d，要求至少: %d 字符。请使用强随机密钥。",
                jwtSecret.length(), MIN_SECRET_LENGTH
            );
            logger.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        // 检查是否使用了默认/示例密钥
        if (jwtSecret.contains("change") || jwtSecret.contains("your-secret-key") ||
            jwtSecret.contains("example") || jwtSecret.contains("test-only")) {
            logger.warn("警告：JWT_SECRET 似乎是默认或示例密钥！生产环境请使用强随机密钥。");
        }

        logger.info("JWT 密钥验证通过（长度: {} 字符）", jwtSecret.length());
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成访问令牌
     */
    public String generateAccessToken(String userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpiration);

        return Jwts.builder()
                .subject(userId)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 生成刷新令牌
     */
    public String generateRefreshToken(String userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpiration);

        return Jwts.builder()
                .subject(userId)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 从令牌获取用户ID
     */
    public String getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    /**
     * 验证令牌
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            logger.debug("JWT token validated successfully");
            return true;
        } catch (ExpiredJwtException e) {
            logger.warn("JWT token is expired: " + e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.warn("JWT token is unsupported: " + e.getMessage());
        } catch (MalformedJwtException e) {
            logger.warn("JWT token is malformed: " + e.getMessage());
        } catch (SecurityException e) {
            logger.warn("JWT token signature validation failed: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.warn("JWT token is illegal: " + e.getMessage());
        }
        return false;
    }

    /**
     * 检查令牌是否过期
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return true;
        }
    }

    /**
     * 获取访问令牌过期时间（秒）
     */
    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpiration / 1000;
    }

    /**
     * 获取签名密钥（供其他服务使用）
     * 安全加固：公开此方法避免其他服务硬编码密钥
     */
    public SecretKey getSigningKeyPublic() {
        return getSigningKey();
    }
}
