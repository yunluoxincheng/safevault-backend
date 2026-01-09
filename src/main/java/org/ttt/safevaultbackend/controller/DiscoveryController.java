package org.ttt.safevaultbackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.ttt.safevaultbackend.dto.request.RegisterLocationRequest;
import org.ttt.safevaultbackend.dto.response.NearbyUserResponse;
import org.ttt.safevaultbackend.service.DiscoveryService;
import org.ttt.safevaultbackend.service.UserService;

import java.util.List;

/**
 * 附近发现控制器
 */
@RestController
@RequestMapping("/v1/discovery")
@RequiredArgsConstructor
@Tag(name = "附近发现", description = "附近用户发现和位置管理")
public class DiscoveryController {

    private final DiscoveryService discoveryService;
    private final UserService userService;

    @PostMapping("/register")
    @Operation(summary = "注册位置", description = "注册当前用户的位置信息用于附近发现")
    public ResponseEntity<Void> registerLocation(@Valid @RequestBody RegisterLocationRequest request) {
        String userId = userService.getCurrentUserId();
        discoveryService.registerLocation(userId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/nearby")
    @Operation(summary = "获取附近用户", description = "获取指定半径内的附近用户列表")
    public ResponseEntity<List<NearbyUserResponse>> getNearbyUsers(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "1000") double radius) {
        String userId = userService.getCurrentUserId();
        List<NearbyUserResponse> response = discoveryService.getNearbyUsers(userId, lat, lng, radius);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/heartbeat")
    @Operation(summary = "发送心跳", description = "更新当前用户的在线状态")
    public ResponseEntity<Void> sendHeartbeat() {
        String userId = userService.getCurrentUserId();
        discoveryService.updateOnlineStatus(userId);
        return ResponseEntity.ok().build();
    }
}
