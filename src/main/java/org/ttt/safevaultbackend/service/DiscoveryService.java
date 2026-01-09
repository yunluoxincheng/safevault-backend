package org.ttt.safevaultbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ttt.safevaultbackend.dto.OnlineUserMessage;
import org.ttt.safevaultbackend.dto.request.RegisterLocationRequest;
import org.ttt.safevaultbackend.dto.response.NearbyUserResponse;
import org.ttt.safevaultbackend.entity.OnlineUser;
import org.ttt.safevaultbackend.entity.User;
import org.ttt.safevaultbackend.exception.ResourceNotFoundException;
import org.ttt.safevaultbackend.repository.OnlineUserRepository;
import org.ttt.safevaultbackend.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 附近发现服务
 */
@Service
@RequiredArgsConstructor
public class DiscoveryService {

    private final OnlineUserRepository onlineUserRepository;
    private final UserRepository userRepository;
    private final WebSocketService webSocketService;

    /**
     * 注册位置信息
     */
    @Transactional
    public void registerLocation(String userId, RegisterLocationRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));

        // 生成会话ID（简化版）
        String sessionId = UUID.randomUUID().toString();

        // 查找是否已存在
        Optional<OnlineUser> existing = onlineUserRepository.findByUserId(userId);
        OnlineUser onlineUser;

        if (existing.isPresent()) {
            onlineUser = existing.get();
            onlineUser.setLatitude(request.getLatitude());
            onlineUser.setLongitude(request.getLongitude());
            onlineUser.setLastSeen(LocalDateTime.now());
            onlineUser.setSessionId(sessionId);
        } else {
            onlineUser = OnlineUser.builder()
                    .userId(userId)
                    .username(user.getUsername())
                    .displayName(user.getDisplayName())
                    .latitude(request.getLatitude())
                    .longitude(request.getLongitude())
                    .lastSeen(LocalDateTime.now())
                    .sessionId(sessionId)
                    .build();
        }

        onlineUserRepository.save(onlineUser);

        // 广播上线通知
        OnlineUserMessage message = OnlineUserMessage.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .lastSeen(System.currentTimeMillis())
                .build();

        webSocketService.broadcastOnlineUser(message);
    }

    /**
     * 获取附近用户
     */
    @Transactional(readOnly = true)
    public List<NearbyUserResponse> getNearbyUsers(String userId, double lat, double lng, double radiusInMeters) {
        // 计算超时阈值（2分钟前）
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(2);

        // 使用 Haversine 公式计算距离
        List<OnlineUser> allOnlineUsers = onlineUserRepository.findAll();
        List<NearbyUserResponse> nearbyUsers = new ArrayList<>();

        for (OnlineUser onlineUser : allOnlineUsers) {
            // 跳过自己
            if (onlineUser.getUserId().equals(userId)) {
                continue;
            }

            // 检查是否活跃
            if (onlineUser.getLastSeen().isBefore(threshold)) {
                continue;
            }

            // 计算距离
            double distance = calculateDistance(
                    lat, lng,
                    onlineUser.getLatitude(), onlineUser.getLongitude()
            );

            // 检查是否在范围内
            if (distance <= radiusInMeters) {
                nearbyUsers.add(NearbyUserResponse.builder()
                        .userId(onlineUser.getUserId())
                        .username(onlineUser.getUsername())
                        .displayName(onlineUser.getDisplayName())
                        .distance(distance)
                        .lastSeen(onlineUser.getLastSeen().toEpochSecond(ZoneOffset.UTC))
                        .build());
            }
        }

        // 按距离排序
        nearbyUsers.sort(Comparator.comparingDouble(NearbyUserResponse::getDistance));

        return nearbyUsers;
    }

    /**
     * 更新在线状态
     */
    @Transactional
    public void updateOnlineStatus(String userId) {
        onlineUserRepository.findByUserId(userId).ifPresent(onlineUser -> {
            onlineUser.setLastSeen(LocalDateTime.now());
            onlineUserRepository.save(onlineUser);
        });
    }

    /**
     * 计算两点间的距离（Haversine 公式）
     * 返回单位：米
     */
    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6371000; // 地球半径（米）

        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }
}
