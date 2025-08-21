package umc.cockple.demo.global.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import umc.cockple.demo.global.response.BaseResponse;
import umc.cockple.demo.global.response.code.status.CommonErrorCode;
import umc.cockple.demo.global.response.code.status.CommonSuccessCode;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin", description = "관리자 API (개발용)")
public class AdminController {

    private final StringRedisTemplate stringRedisTemplate;
    private final CacheManager cacheManager;

    @GetMapping("/redis/health")
    @Operation(summary = "Redis 연결 상태 확인", description = "Redis가 살아있는지 간단히 확인합니다.")
    public BaseResponse<Map<String, Object>> checkRedisHealth() {
        Map<String, Object> result = new HashMap<>();

        try {
            String pong = stringRedisTemplate.getConnectionFactory()
                    .getConnection()
                    .ping();

            result.put("status", "UP");
            result.put("ping", pong);

            log.info("Redis 헬스체크 성공");
            return BaseResponse.success(CommonSuccessCode.OK, result);

        } catch (Exception e) {
            log.error("Redis 헬스체크 실패", e);

            result.put("status", "DOWN");
            result.put("error", e.getMessage());

            return BaseResponse.error(CommonErrorCode.SERVICE_UNAVAILABLE, "Redis 연결 실패");
        }
    }

    @PostMapping("/cache/clear-all")
    @Operation(summary = "모든 캐시 삭제", description = "Redis의 모든 캐시와 Spring Cache를 삭제합니다.")
    public BaseResponse<Map<String, Object>> clearAllCache() {
        try {
            Map<String, Object> result = new HashMap<>();

            Set<String> redisKeys = stringRedisTemplate.keys("*");
            if (redisKeys != null && !redisKeys.isEmpty()) {
                stringRedisTemplate.delete(redisKeys);
            }

            for (String cacheName : cacheManager.getCacheNames()) {
                Cache cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                    log.info("Spring Cache '{}' 삭제 완료", cacheName);
                }
            }

            result.put("message", "모든 캐시 삭제 완료");

            return BaseResponse.success(CommonSuccessCode.OK, result);

        } catch (Exception e) {
            log.error("모든 캐시 삭제 실패", e);
            return BaseResponse.error(null, "캐시 삭제 실패: " + e.getMessage());
        }
    }
}
