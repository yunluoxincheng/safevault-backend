package org.ttt.safevaultbackend.entity;

/**
 * 分享类型枚举
 */
public enum ShareType {
    /**
     * 直接分享（链接/二维码）
     */
    DIRECT,

    /**
     * 用户对用户分享（通过用户ID）
     */
    USER_TO_USER,

    /**
     * 附近设备分享
     */
    NEARBY
}
