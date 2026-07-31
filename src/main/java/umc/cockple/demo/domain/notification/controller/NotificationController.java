package umc.cockple.demo.domain.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import umc.cockple.demo.domain.notification.dto.FcmTokenRequestDTO;
import umc.cockple.demo.domain.notification.dto.NotificationListResponseDTO;
import umc.cockple.demo.domain.notification.dto.NotificationV2ListResponseDTO;
import umc.cockple.demo.domain.notification.fcm.FcmService;
import umc.cockple.demo.domain.notification.service.NotificationCommandService;
import umc.cockple.demo.domain.notification.dto.ExistNewNotificationResponseDTO;
import umc.cockple.demo.domain.notification.service.NotificationQueryService;
import umc.cockple.demo.global.response.BaseResponse;
import umc.cockple.demo.global.response.code.status.CommonSuccessCode;
import umc.cockple.demo.global.security.utils.SecurityUtil;

import static umc.cockple.demo.domain.notification.dto.MarkAsReadDTO.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
@Tag(name = "Notification", description = "알림 API")
public class NotificationController {

    private final NotificationQueryService notificationQueryService;
    private final NotificationCommandService notificationCommandService;
    private final FcmService fcmService;

    @GetMapping("/v2/notifications")
    @Operation(summary = "내 알림 전체 조회 v2",
            description = "destination(resourceType, resourceId, action) 기반으로 알림을 조회합니다.")
    public BaseResponse<NotificationV2ListResponseDTO> getAllNotificationsV2(
            @RequestParam(required = false) @Positive Long cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        Long memberId = SecurityUtil.getCurrentMemberId();

        return BaseResponse.success(CommonSuccessCode.OK,
                notificationQueryService.getAllNotificationsV2(memberId, cursor, size));
    }

    /**
     * @deprecated [사용금지 예정] destination 기반 v2 알림 조회 API로 대체됩니다.
     */
    @GetMapping("/notifications")
    @Operation(summary = "[사용금지 예정] 내 알림 전체 조회",
            description = "destination 기반 v2 알림 조회 API로 대체됩니다. 신규 클라이언트는 이 API를 사용하지 마세요. "
                    + "첫 페이지는 cursor 생략, 다음 페이지는 직전 응답의 nextCursor를 전달합니다.",
            deprecated = true)
    @Deprecated(since = "v2", forRemoval = true)
    public BaseResponse<NotificationListResponseDTO> getAllNotifications(
            @RequestParam(required = false) @Positive Long cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        Long memberId = SecurityUtil.getCurrentMemberId();

        return BaseResponse.success(CommonSuccessCode.OK,
                notificationQueryService.getAllNotifications(memberId, cursor, size));
    }



    @GetMapping("/notifications/count")
    @Operation(summary = "안 읽은 알림 존재여부 조회",
            description = "사용자가 읽지 않은 알림이 있는지 확인합니다. 존재 시 알림 아이콘에 빨간 점이 표시됩니다 ")
    public BaseResponse<ExistNewNotificationResponseDTO> checkUnReadNotification() {

        Long memberId = SecurityUtil.getCurrentMemberId();

        return BaseResponse.success(CommonSuccessCode.OK,notificationQueryService.checkUnreadNotification(memberId));
    }



    /**
     * @deprecated [사용금지 예정] type을 받지 않는 destination 기반 v2 읽음 처리 API로 대체됩니다.
     */
    @PatchMapping("/notifications/{notificationId}")
    @Operation(summary = "[사용금지 예정] 내 특정 알림 조회 및 읽음 처리",
            description = "type을 받지 않는 destination 기반 v2 읽음 처리 API로 대체됩니다. 신규 클라이언트는 이 API를 사용하지 마세요. ",
            deprecated = true)
    @Deprecated(since = "v2", forRemoval = true)
    public BaseResponse<Response> markAsReadNotification(@PathVariable Long notificationId,
                                                         Request type) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        return BaseResponse.success(CommonSuccessCode.OK, notificationCommandService.markAsReadNotification(memberId, notificationId, type.type()));
    }


    @PatchMapping("/v2/notifications/{notificationId}/read")
    @Operation(summary = "알림 읽음 처리 v2",
            description = "알림의 읽음 상태만 변경합니다. 초대 수락·거절 상태는 초대 API에서 처리합니다.")
    public BaseResponse<Void> markAsReadNotificationV2(@PathVariable Long notificationId) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        notificationCommandService.markAsReadNotificationV2(memberId, notificationId);
        return BaseResponse.success(CommonSuccessCode.OK, null);
    }

    // ========== FCM 토큰 등록/갱신 API ==========
    @PatchMapping("/notifications/fcm-token")
    @Operation(summary = "FCM 토큰 등록",
            description = "디바이스의 FCM 토큰을 등록하거나 갱신합니다. 알림 권한 거부 시 null을 전달해 토큰을 삭제합니다.")
    public BaseResponse<Void> registerFcmToken(@RequestBody @Valid FcmTokenRequestDTO request) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        fcmService.registerFcmToken(memberId, request.fcmToken());
        return BaseResponse.success(CommonSuccessCode.OK, null);
    }


}
