package umc.cockple.demo.domain.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import umc.cockple.demo.domain.notification.domain.Notification;
import umc.cockple.demo.domain.notification.dto.AllNotificationsResponseDTO;
import umc.cockple.demo.domain.notification.service.NotificationCommandService;
import umc.cockple.demo.domain.notification.service.NotificationQueryService;
import umc.cockple.demo.global.response.BaseResponse;
import umc.cockple.demo.global.response.code.status.CommonSuccessCode;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
@Tag(name = "Notification", description = "알림 API")
public class NotificationController {

    private final NotificationQueryService notificationQueryService;
    private final NotificationCommandService notificationCommandService;

    @GetMapping("/notifications")
    @Operation(summary = "내 알림 전체 조회",
            description = "사용자에게 온 알림 전체를 조회합니다. ")
    public BaseResponse<List<AllNotificationsResponseDTO>> getAllNotifications() {
        // 추후 시큐리티를 통해 id 가져옴
        Long memberId = 1L;

        return BaseResponse.success(CommonSuccessCode.OK, notificationQueryService.getAllNotifications(memberId));
    }


    @PatchMapping("/notifications/{notificationId}")
    @Operation(summary = "내 특정 알림 조회 및 읽음 처리",
            description = "특정 알림을 조회하고 읽음 처리를 진행합니다. ")
    public BaseResponse<String> markAsReadNotification(@PathVariable Long notificationId) {
        // 추후 시큐리티를 통해 id 가져옴
        Long memberId = 1L;

        notificationCommandService.markAsReadNotification(notificationId);
        return BaseResponse.success(CommonSuccessCode.NO_CONTENT);
    }


}
