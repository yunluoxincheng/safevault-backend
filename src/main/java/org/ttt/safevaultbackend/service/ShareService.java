package org.ttt.safevaultbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.ttt.safevaultbackend.dto.*;
import org.ttt.safevaultbackend.dto.request.CreateShareRequest;
import org.ttt.safevaultbackend.dto.response.ReceivedShareResponse;
import org.ttt.safevaultbackend.dto.response.ShareResponse;
import org.ttt.safevaultbackend.entity.*;
import org.ttt.safevaultbackend.exception.BusinessException;
import org.ttt.safevaultbackend.exception.ResourceNotFoundException;
import org.ttt.safevaultbackend.repository.PasswordShareRepository;
import org.ttt.safevaultbackend.repository.ShareAuditLogRepository;
import org.ttt.safevaultbackend.repository.UserRepository;
import org.ttt.safevaultbackend.security.JwtTokenProvider;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 分享服务
 */
@Service
@RequiredArgsConstructor
public class ShareService {

    private final PasswordShareRepository shareRepository;
    private final UserRepository userRepository;
    private final ShareAuditLogRepository auditLogRepository;
    private final CryptoService cryptoService;
    private final JwtTokenProvider tokenProvider;
    private final WebSocketService webSocketService;

    /**
     * 创建分享（支持三种类型）
     */
    @Transactional
    public ShareResponse createShare(CreateShareRequest request, String fromUserId) {
        User fromUser = userRepository.findById(fromUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", fromUserId));

        // 根据分享类型处理
        return switch (request.getShareType()) {
            case DIRECT -> createDirectShare(request, fromUser);
            case USER_TO_USER -> createUserShare(request, fromUser);
            case NEARBY -> createNearbyShare(request, fromUser);
        };
    }

    /**
     * 创建直接分享（链接/二维码）
     */
    private ShareResponse createDirectShare(CreateShareRequest request, User fromUser) {
        String shareId = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(request.getExpireInMinutes());

        // 构建加密数据
        Map<String, String> encryptedData = buildEncryptedData(request);

        // 创建分享记录（不指定接收方）
        PasswordShare share = PasswordShare.builder()
                .shareId(shareId)
                .passwordId(request.getPasswordId())
                .fromUser(fromUser)
                .toUser(null)
                .encryptedData(serializeEncryptedData(encryptedData))
                .createdAt(LocalDateTime.now())
                .expiresAt(expiresAt)
                .canView(request.getPermission().isCanView())
                .canSave(request.getPermission().isCanSave())
                .isRevocable(request.getPermission().isRevocable())
                .status(ShareStatus.ACTIVE)
                .shareType(ShareType.DIRECT)
                .build();

        share = shareRepository.save(share);

        // 记录审计日志
        logShareAction(share, "CREATE_DIRECT_SHARE", fromUser.getUserId());

        // 广播分享通知给所有在线用户（除了发送者自己）
        ShareNotificationMessage notification = ShareNotificationMessage.builder()
                .type("NEW_DIRECT_SHARE")
                .shareId(shareId)
                .fromUserId(fromUser.getUserId())
                .fromDisplayName(fromUser.getDisplayName())
                .message(String.format("%s 分享了一个密码", fromUser.getDisplayName()))
                .timestamp(System.currentTimeMillis())
                .build();

        // 遍历所有在线用户发送通知
        webSocketService.broadcastShareNotification(notification);

        // 生成分享 Token
        SharePackage sharePackage = SharePackage.builder()
                .shareId(shareId)
                .fromUserId(fromUser.getUserId())
                .encryptedData(encryptedData)
                .permission(request.getPermission())
                .expiresAt(expiresAt.toEpochSecond(ZoneOffset.UTC))
                .build();

        String shareToken = sharePackage.toBase64Token();

        return ShareResponse.builder()
                .shareId(shareId)
                .shareToken(shareToken)
                .expiresAt(expiresAt.toEpochSecond(ZoneOffset.UTC))
                .build();
    }

    /**
     * 创建用户对用户分享
     */
    private ShareResponse createUserShare(CreateShareRequest request, User fromUser) {
        if (request.getToUserId() == null) {
            throw new BusinessException("MISSING_TO_USER", "用户对用户分享需要指定接收方");
        }

        User toUser = userRepository.findById(request.getToUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", request.getToUserId()));

        String shareId = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(request.getExpireInMinutes());

        // 构建加密数据
        Map<String, String> encryptedData = buildEncryptedData(request);

        // 创建分享记录
        PasswordShare share = PasswordShare.builder()
                .shareId(shareId)
                .passwordId(request.getPasswordId())
                .fromUser(fromUser)
                .toUser(toUser)
                .encryptedData(serializeEncryptedData(encryptedData))
                .createdAt(LocalDateTime.now())
                .expiresAt(expiresAt)
                .canView(request.getPermission().isCanView())
                .canSave(request.getPermission().isCanSave())
                .isRevocable(request.getPermission().isRevocable())
                .status(ShareStatus.PENDING)
                .shareType(ShareType.USER_TO_USER)
                .build();

        share = shareRepository.save(share);

        // 记录审计日志
        logShareAction(share, "CREATE_USER_SHARE", fromUser.getUserId());

        // 发送实时通知
        ShareNotificationMessage notification = ShareNotificationMessage.builder()
                .type("NEW_SHARE")
                .shareId(shareId)
                .fromUserId(fromUser.getUserId())
                .fromDisplayName(fromUser.getDisplayName())
                .message(String.format("%s 向你分享了一个密码", fromUser.getDisplayName()))
                .timestamp(System.currentTimeMillis())
                .build();

        webSocketService.sendShareNotification(toUser.getUserId(), notification);

        return ShareResponse.builder()
                .shareId(shareId)
                .shareToken(null)
                .expiresAt(expiresAt.toEpochSecond(ZoneOffset.UTC))
                .build();
    }

    /**
     * 创建附近设备分享
     */
    private ShareResponse createNearbyShare(CreateShareRequest request, User fromUser) {
        // 附近分享本质上也是用户对用户分享
        return createUserShare(request, fromUser);
    }

    /**
     * 接收分享
     */
    @Transactional(readOnly = true)
    public ReceivedShareResponse receiveShare(String shareId, String userId) {
        PasswordShare share = shareRepository.findById(shareId)
                .orElseThrow(() -> new ResourceNotFoundException("Share", "shareId", shareId));

        // 检查权限
        if (share.getToUser() != null && !share.getToUser().getUserId().equals(userId)) {
            throw new BusinessException("ACCESS_DENIED", "无权访问此分享");
        }

        // 检查状态
        if (share.getStatus() == ShareStatus.EXPIRED) {
            throw new BusinessException("SHARE_EXPIRED", "分享已过期");
        }
        if (share.getStatus() == ShareStatus.REVOKED) {
            throw new BusinessException("SHARE_REVOKED", "分享已被撤销");
        }
        if (share.getStatus() == ShareStatus.ACCEPTED) {
            throw new BusinessException("SHARE_ALREADY_ACCEPTED", "分享已被接收");
        }

        // 解析加密数据
        Map<String, String> encryptedData = deserializeEncryptedData(share.getEncryptedData());
        PasswordData passwordData = PasswordData.builder()
                .title(encryptedData.get("title"))
                .username(encryptedData.get("username"))
                .encryptedPassword(encryptedData.get("password"))
                .url(encryptedData.get("url"))
                .notes(encryptedData.get("notes"))
                .build();

        SharePermission permission = SharePermission.builder()
                .canView(share.isCanView())
                .canSave(share.isCanSave())
                .isRevocable(share.isRevocable())
                .build();

        return ReceivedShareResponse.builder()
                .shareId(share.getShareId())
                .fromUserId(share.getFromUser().getUserId())
                .fromDisplayName(share.getFromUser().getDisplayName())
                .passwordData(passwordData)
                .permission(permission)
                .status(share.getStatus())
                .shareType(share.getShareType())
                .createdAt(share.getCreatedAt().toEpochSecond(ZoneOffset.UTC))
                .expiresAt(share.getExpiresAt().toEpochSecond(ZoneOffset.UTC))
                .build();
    }

    /**
     * 撤销分享
     */
    @Transactional
    public void revokeShare(String shareId, String userId) {
        PasswordShare share = shareRepository.findById(shareId)
                .orElseThrow(() -> new ResourceNotFoundException("Share", "shareId", shareId));

        // 检查权限
        if (!share.getFromUser().getUserId().equals(userId)) {
            throw new BusinessException("ACCESS_DENIED", "无权撤销此分享");
        }

        if (!share.isRevocable()) {
            throw new BusinessException("SHARE_NOT_REVOCABLE", "此分享不可撤销");
        }

        share.setStatus(ShareStatus.REVOKED);
        shareRepository.save(share);

        logShareAction(share, "REVOKE_SHARE", userId);

        // 通知接收方
        if (share.getToUser() != null) {
            ShareNotificationMessage notification = ShareNotificationMessage.builder()
                    .type("SHARE_REVOKED")
                    .shareId(shareId)
                    .fromUserId(userId)
                    .fromDisplayName(share.getFromUser().getDisplayName())
                    .message("分享已被撤销")
                    .timestamp(System.currentTimeMillis())
                    .build();

            webSocketService.sendShareNotification(share.getToUser().getUserId(), notification);
        }
    }

    /**
     * 保存分享的密码
     */
    @Transactional
    public void saveSharedPassword(String shareId, String userId) {
        PasswordShare share = shareRepository.findById(shareId)
                .orElseThrow(() -> new ResourceNotFoundException("Share", "shareId", shareId));

        // 检查权限
        if (!share.isCanSave()) {
            throw new BusinessException("SAVE_NOT_ALLOWED", "此分享不允许保存");
        }

        // 检查访问权限
        if (share.getToUser() != null) {
            // 用户对用户分享，需要验证接收方
            if (!share.getToUser().getUserId().equals(userId)) {
                throw new BusinessException("ACCESS_DENIED", "无权访问此分享");
            }
            // 更新状态为已接受
            share.setStatus(ShareStatus.ACCEPTED);
            shareRepository.save(share);
        }
        // 云端直接分享（toUser 为 null），所有人都可以保存，不需要更新状态

        logShareAction(share, "SAVE_SHARED_PASSWORD", userId);
    }

    /**
     * 获取创建的分享
     */
    @Transactional(readOnly = true)
    public List<ReceivedShareResponse> getMyShares(String userId) {
        List<PasswordShare> shares = shareRepository.findByFromUser_UserIdOrderByCreatedAtDesc(userId);

        return shares.stream()
                .map(this::mapToShareResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取接收的分享
     * 包含：1. 用户对用户分享 2. 所有活跃的云端直接分享
     */
    @Transactional(readOnly = true)
    public List<ReceivedShareResponse> getReceivedShares(String userId) {
        // 获取用户接收的分享（用户对用户、附近用户）
        List<PasswordShare> userShares = shareRepository.findByToUser_UserIdOrderByCreatedAtDesc(userId);

        // 获取所有活跃的云端直接分享（所有人可见）
        List<PasswordShare> directShares = shareRepository.findActiveDirectShares(LocalDateTime.now());

        // 合并并去重（使用 Set 避免重复）
        List<PasswordShare> allShares = new java.util.ArrayList<>(userShares);

        // 添加云端直接分享（排除自己创建的）
        for (PasswordShare directShare : directShares) {
            if (!directShare.getFromUser().getUserId().equals(userId)) {
                allShares.add(directShare);
            }
        }

        // 按创建时间排序
        allShares.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

        return allShares.stream()
                .map(this::mapToShareResponse)
                .collect(Collectors.toList());
    }

    /**
     * 记录分享操作日志
     */
    private void logShareAction(PasswordShare share, String action, String performedBy) {
        ShareAuditLog log = ShareAuditLog.builder()
                .share(share)
                .action(action)
                .actionPerformedAt(LocalDateTime.now())
                .performedBy(performedBy)
                .build();

        auditLogRepository.save(log);
    }

    /**
     * 构建加密数据
     */
    private Map<String, String> buildEncryptedData(CreateShareRequest request) {
        Map<String, String> data = new HashMap<>();
        data.put("title", request.getTitle());
        data.put("username", request.getUsername() != null ? request.getUsername() : "");
        data.put("password", request.getEncryptedPassword());
        data.put("url", request.getUrl() != null ? request.getUrl() : "");
        data.put("notes", request.getNotes() != null ? request.getNotes() : "");
        return data;
    }

    /**
     * 序列化加密数据
     */
    private String serializeEncryptedData(Map<String, String> data) {
        try {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : data.entrySet()) {
                if (sb.length() > 0) sb.append("|");
                sb.append(entry.getKey()).append("=").append(entry.getValue());
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize encrypted data", e);
        }
    }

    /**
     * 反序列化加密数据
     */
    private Map<String, String> deserializeEncryptedData(String serialized) {
        Map<String, String> data = new HashMap<>();
        if (serialized != null && !serialized.isEmpty()) {
            String[] pairs = serialized.split("\\|");
            for (String pair : pairs) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) {
                    data.put(kv[0], kv[1]);
                }
            }
        }
        return data;
    }

    /**
     * 映射到分享响应
     */
    private ReceivedShareResponse mapToShareResponse(PasswordShare share) {
        Map<String, String> encryptedData = deserializeEncryptedData(share.getEncryptedData());
        PasswordData passwordData = PasswordData.builder()
                .title(encryptedData.get("title"))
                .username(encryptedData.get("username"))
                .encryptedPassword(encryptedData.get("password"))
                .url(encryptedData.get("url"))
                .notes(encryptedData.get("notes"))
                .build();

        SharePermission permission = SharePermission.builder()
                .canView(share.isCanView())
                .canSave(share.isCanSave())
                .isRevocable(share.isRevocable())
                .build();

        return ReceivedShareResponse.builder()
                .shareId(share.getShareId())
                .fromUserId(share.getFromUser() != null ? share.getFromUser().getUserId() : null)
                .fromDisplayName(share.getFromUser() != null ? share.getFromUser().getDisplayName() : null)
                .passwordData(passwordData)
                .permission(permission)
                .status(share.getStatus())
                .shareType(share.getShareType())
                .createdAt(share.getCreatedAt().toEpochSecond(ZoneOffset.UTC))
                .expiresAt(share.getExpiresAt().toEpochSecond(ZoneOffset.UTC))
                .build();
    }
}
