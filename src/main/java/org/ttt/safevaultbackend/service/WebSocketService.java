package org.ttt.safevaultbackend.service;

import org.ttt.safevaultbackend.dto.OnlineUserMessage;
import org.ttt.safevaultbackend.dto.ShareNotificationMessage;

/**
 * WebSocket 服务接口
 */
public interface WebSocketService {

    /**
     * 发送分享通知给指定用户
     */
    void sendShareNotification(String userId, ShareNotificationMessage notification);

    /**
     * 广播分享通知给所有用户
     */
    void broadcastShareNotification(ShareNotificationMessage notification);

    /**
     * 广播在线用户状态
     */
    void broadcastOnlineUser(OnlineUserMessage message);

    /**
     * 处理连接
     */
    void handleConnect(String userId, String sessionId);

    /**
     * 处理断开
     */
    void handleDisconnect(String userId, String sessionId);

    /**
     * 处理心跳
     */
    void handleHeartbeat(String userId);
}
