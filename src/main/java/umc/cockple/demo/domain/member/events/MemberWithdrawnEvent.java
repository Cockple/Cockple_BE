package umc.cockple.demo.domain.member.events;

import java.time.LocalDateTime;

public record MemberWithdrawnEvent(
        Long memberId,
        LocalDateTime occurredAt
) {
    public static MemberWithdrawnEvent withdrawn(Long memberId) {
        return new MemberWithdrawnEvent(
                memberId,
                LocalDateTime.now()
        );
    }
}
