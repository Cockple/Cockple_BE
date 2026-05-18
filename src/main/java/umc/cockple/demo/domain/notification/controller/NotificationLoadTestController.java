package umc.cockple.demo.domain.notification.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.exception.MemberException;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.notification.dto.CreateNotificationRequestDTO;
import umc.cockple.demo.domain.notification.enums.NotificationTarget;
import umc.cockple.demo.domain.notification.service.NotificationCommandService;
import umc.cockple.demo.global.response.BaseResponse;
import umc.cockple.demo.global.response.code.status.CommonSuccessCode;
import umc.cockple.demo.global.security.utils.SecurityUtil;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Profile("staging")
public class NotificationLoadTestController {

    private final NotificationCommandService notificationCommandService;
    private final MemberRepository memberRepository;

    @PostMapping("/notification")
    public BaseResponse<Void> testNotification(@RequestParam Long partyId) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        notificationCommandService.createNotification(
                CreateNotificationRequestDTO.builder()
                        .member(member)
                        .partyId(partyId)
                        .target(NotificationTarget.PARTY_MODIFY)
                        .build()
        );

        return BaseResponse.success(CommonSuccessCode.OK, null);
    }
}
