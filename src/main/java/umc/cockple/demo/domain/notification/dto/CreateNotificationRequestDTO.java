package umc.cockple.demo.domain.notification.dto;

/*
* 운동 : 운동 삭제의 경우를 제외하고 해당 모임 페이지의 운동 위치로 이동 -> 운동id 필요
*       운동 수정, 운동 삭제의 경우 운동 날짜 (MM.dd (요일)) 띄움 -> 운동 날짜, 요일 필요
* 모임 : 모임 삭제의 경우를 제외하고 해당 모임 페이지로 이동
*
* <타입>
* 모임 초대 : INVITE
* 운동, 모임 삭제 : SIMPLE
* 모임 승인, 모임 수정, 운동 수정, 운동 참석 변경 : CHANGE
* */

import jakarta.persistence.*;
import lombok.Builder;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.notification.enums.NotificationTarget;
import umc.cockple.demo.domain.notification.enums.NotificationType;

import java.time.LocalDate;

@Builder
public record CreateNotificationRequestDTO(
        Member member,
        Long partyId,
        Long exerciseId, // 운동 관련이 아니면 필요 X
        LocalDate exerciseDate, // 운동 관련이 아니면 필요 X
        Long invitationId, // INVITE 외 타입에서는 필요 X
        String subjectName, // APPROVED 외 타입에서는 필요 X
        NotificationTarget target
) {
}
