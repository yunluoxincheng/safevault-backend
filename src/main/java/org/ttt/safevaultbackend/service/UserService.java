package org.ttt.safevaultbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ttt.safevaultbackend.dto.PasswordData;
import org.ttt.safevaultbackend.dto.response.*;
import org.ttt.safevaultbackend.entity.User;
import org.ttt.safevaultbackend.entity.ShareType;
import org.ttt.safevaultbackend.exception.ResourceNotFoundException;
import org.ttt.safevaultbackend.repository.UserRepository;
import org.ttt.safevaultbackend.repository.PasswordShareRepository;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 用户服务
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordShareRepository shareRepository;

    /**
     * 获取当前用户ID
     */
    public String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof String) {
            return (String) authentication.getPrincipal();
        }
        throw new ResourceNotFoundException("User", "current", "N/A");
    }

    /**
     * 获取当前用户配置
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile() {
        String userId = getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));

        int shareCount = shareRepository.findByFromUser_UserIdOrderByCreatedAtDesc(userId).size();

        return UserProfileResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .publicKey(user.getPublicKey())
                .createdAt(user.getCreatedAt().toEpochSecond(ZoneOffset.UTC))
                .shareCount(shareCount)
                .build();
    }

    /**
     * 通过ID获取用户
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getUserById(String targetUserId) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", targetUserId));

        int shareCount = shareRepository.findByFromUser_UserIdOrderByCreatedAtDesc(targetUserId).size();

        return UserProfileResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .publicKey(user.getPublicKey())
                .createdAt(user.getCreatedAt().toEpochSecond(ZoneOffset.UTC))
                .shareCount(shareCount)
                .build();
    }

    /**
     * 搜索用户
     */
    @Transactional(readOnly = true)
    public List<UserSearchResponse> searchUsers(String query) {
        List<User> users = userRepository.searchByUserIdOrUsername(query);

        return users.stream()
                .map(user -> UserSearchResponse.builder()
                        .userId(user.getUserId())
                        .username(user.getUsername())
                        .displayName(user.getDisplayName())
                        .publicKey(user.getPublicKey())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 更新显示名称
     */
    @Transactional
    public UserProfileResponse updateDisplayName(String displayName) {
        String userId = getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));

        user.setDisplayName(displayName);
        user = userRepository.save(user);

        return UserProfileResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .publicKey(user.getPublicKey())
                .createdAt(user.getCreatedAt().toEpochSecond(ZoneOffset.UTC))
                .shareCount(0)
                .build();
    }

    /**
     * 生成二维码数据（用于扫码分享）
     */
    @Transactional(readOnly = true)
    public QRCodeResponse generateQRCode() {
        String userId = getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));

        // 生成临时 Token
        String tempToken = UUID.randomUUID().toString();

        // 二维码数据格式：safevault:receive:userId:tempToken
        String qrCodeData = String.format("safevault:receive:%s:%s", user.getUserId(), tempToken);

        // 有效期：30分钟
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(30);

        return QRCodeResponse.builder()
                .qrCodeData(qrCodeData)
                .expiresAt(expiresAt.toEpochSecond(ZoneOffset.UTC))
                .size(300)
                .build();
    }

    /**
     * 获取用户统计
     */
    @Transactional(readOnly = true)
    public PasswordData getUserStatistics() {
        String userId = getCurrentUserId();

        long createdSharesCount = shareRepository.findByFromUser_UserIdOrderByCreatedAtDesc(userId).size();
        long receivedSharesCount = shareRepository.findByToUser_UserIdOrderByCreatedAtDesc(userId).size();

        return PasswordData.builder()
                .title("用户统计")
                .username(String.format("创建分享: %d", createdSharesCount))
                .encryptedPassword(String.format("接收分享: %d", receivedSharesCount))
                .build();
    }
}
