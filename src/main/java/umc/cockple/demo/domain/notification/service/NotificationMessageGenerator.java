package umc.cockple.demo.domain.notification.service;

import org.springframework.stereotype.Component;

@Component
public class NotificationMessageGenerator {

    public String generateInviteMessage(String partyName) {
        return String.format("'%s' 모임에 초대를 받았습니다.", partyName);
    }

    public String generateInviteAcceptedMessage() {
        return "모임 가입이 승인되었어요!";
    }

    public String generatePartyInfoChangedMessage() {
        return "모임 정보가 수정되었어요!";
    }

    public String generatePartyDeletedMessage() {
        return "모임이 삭제되었어요!";
    }

    public String generateExerciseDeletedMessage(String dateStr) {
        return String.format("%s 운동이 삭제되었어요!", dateStr);
    }

    public String generateExerciseChangedMessage(String dateStr) {
        return String.format("%s 운동이 수정되었습니다.", dateStr);
    }

    public String generateExerciseAttendChangedMessage() {
        return "운동 참석으로 변경되었어요!";
    }
}
