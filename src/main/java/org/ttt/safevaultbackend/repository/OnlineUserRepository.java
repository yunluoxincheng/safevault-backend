package org.ttt.safevaultbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.ttt.safevaultbackend.entity.OnlineUser;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 在线用户数据访问层
 */
@Repository
public interface OnlineUserRepository extends JpaRepository<OnlineUser, String> {

    /**
     * 通过用户ID查找在线用户
     */
    Optional<OnlineUser> findByUserId(String userId);

    /**
     * 通过会话ID查找在线用户
     */
    Optional<OnlineUser> findBySessionId(String sessionId);

    /**
     * 查找超时的用户（用于定时清理）
     */
    List<OnlineUser> findByLastSeenBefore(LocalDateTime threshold);

    /**
     * 查找附近的在线用户
     * 使用 Haversine 公式计算地球表面两点间的距离
     */
    @Query(value = """
        SELECT * FROM online_users
        WHERE last_seen > :threshold
        AND earth_distance(
            ll_to_earth(latitude, longitude),
            ll_to_earth(:lat, :lng)
        ) <= :radius
        ORDER BY last_seen DESC
        """, nativeQuery = true)
    List<OnlineUser> findNearbyUsers(
            @Param("lat") double latitude,
            @Param("lng") double longitude,
            @Param("radius") double radiusInMeters,
            @Param("threshold") LocalDateTime threshold
    );

    /**
     * 删除超时的在线用户
     */
    void deleteByLastSeenBefore(LocalDateTime threshold);
}
