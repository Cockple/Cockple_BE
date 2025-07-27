package umc.cockple.demo.domain.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import umc.cockple.demo.domain.notification.domain.Notification;
import umc.cockple.demo.domain.notification.dto.AllNotificationsResponseDTO;
import umc.cockple.demo.domain.notification.dto.ExistNewNotificationResponseDTO;
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

    @GetMapping("/notifications")
    @Operation(summary = "내 알림 전체 조회",
            description = "사용자에게 온 알림 전체를 조회합니다. ")
    public BaseResponse<List<AllNotificationsResponseDTO>> getAllNotifications() {
        // 추후 시큐리티를 통해 id 가져옴
        Long memberId = 1L;

        return BaseResponse.success(CommonSuccessCode.OK, notificationQueryService.getAllNotifications(memberId));
    }

    @GetMapping("/notifications/count")
    @Operation(summary = "안 읽은 알림 존재여부 조회",
            description = "사용자가 읽지 않은 알림이 있는지 확인합니다. 존재 시 알림 아이콘에 빨간 점이 표시됩니다 ")
    public BaseResponse<ExistNewNotificationResponseDTO> checkUnReadNotification() {
        // 추후 시큐리티를 통해 id 가져옴
        Long memberId = 1L;

        return BaseResponse.success(CommonSuccessCode.OK,notificationQueryService.checkUnreadNotification(memberId));
    }

}
