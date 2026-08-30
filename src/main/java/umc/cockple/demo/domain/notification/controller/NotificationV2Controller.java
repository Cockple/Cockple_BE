package umc.cockple.demo.domain.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import umc.cockple.demo.domain.notification.dto.NotificationV2ListResponseDTO;
import umc.cockple.demo.domain.notification.service.NotificationV2CommandService;
import umc.cockple.demo.domain.notification.service.NotificationV2QueryService;
import umc.cockple.demo.global.response.BaseResponse;
import umc.cockple.demo.global.response.code.status.CommonSuccessCode;
import umc.cockple.demo.global.security.utils.SecurityUtil;

@RestController
@RequestMapping("/api/v2/notifications")
@RequiredArgsConstructor
@Validated
@Tag(name = "Notification V2", description = "destination 기반 알림 API")
public class NotificationV2Controller {

    private final NotificationV2QueryService notificationV2QueryService;
    private final NotificationV2CommandService notificationV2CommandService;

    @GetMapping
    @Operation(summary = "내 알림 전체 조회 v2",
            description = "destination(resourceType, resourceId, action) 기반으로 알림을 조회합니다.")
    public BaseResponse<NotificationV2ListResponseDTO> getAllNotifications(
            @RequestParam(required = false) @Positive Long cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return BaseResponse.success(
                CommonSuccessCode.OK,
                notificationV2QueryService.getAllNotifications(
                        SecurityUtil.getCurrentMemberId(), cursor, size));
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "알림 읽음 처리 v2",
            description = "알림의 읽음 상태만 변경합니다. 초대 수락·거절 상태는 초대 API에서 처리합니다.")
    public BaseResponse<Void> markAsRead(@PathVariable Long notificationId) {
        notificationV2CommandService.markAsRead(
                SecurityUtil.getCurrentMemberId(), notificationId);
        return BaseResponse.success(CommonSuccessCode.OK, null);
    }
}
