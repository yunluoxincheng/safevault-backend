package org.ttt.safevaultbackend.entity;

/**
 * 分享状态枚举
 */
public enum ShareStatus {
    /**
     * 待接收
     */
    PENDING,

    /**
     * 活跃中
     */
    ACTIVE,

    /**
     * 已接收
     */
    ACCEPTED,

    /**
     * 已过期
     */
    EXPIRED,

    /**
     * 已撤销
     */
    REVOKED
}
