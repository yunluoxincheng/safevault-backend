package org.ttt.safevaultbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.ttt.safevaultbackend.entity.User;

import java.util.List;
import java.util.Optional;

/**
 * 用户数据访问层
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

    /**
     * 通过设备ID查找用户
     */
    Optional<User> findByDeviceId(String deviceId);

    /**
     * 通过用户名查找用户
     */
    Optional<User> findByUsername(String username);

    /**
     * 通过用户ID查找用户
     */
    Optional<User> findByUserId(String userId);

    /**
     * 通过用户名模糊搜索
     */
    List<User> findByUsernameContainingIgnoreCase(String username);

    /**
     * 通过用户ID或用户名模糊搜索
     */
    @Query("SELECT u FROM User u WHERE u.userId = :query OR LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<User> searchByUserIdOrUsername(@Param("query") String query);

    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 检查用户ID是否存在
     */
    boolean existsByUserId(String userId);

    /**
     * 检查设备ID是否存在
     */
    boolean existsByDeviceId(String deviceId);
}
