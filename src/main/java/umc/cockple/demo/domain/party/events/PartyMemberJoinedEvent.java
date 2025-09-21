package umc.cockple.demo.domain.party.events;

import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.party.enums.PartyMemberAction;

import java.time.LocalDateTime;

public record PartyMemberJoinedEvent(
        Long partyId,
        Long memberId,
        String memberName,
        PartyMemberAction action,
        LocalDateTime occurredAt
) {
    public static PartyMemberJoinedEvent joined(Long partyId, Member member) {
        return new PartyMemberJoinedEvent(
                partyId, member.getId(), member.getMemberName(),
                PartyMemberAction.JOINED, LocalDateTime.now()
        );
    }

    public static PartyMemberJoinedEvent left(Long partyId, Member member) {
        return new PartyMemberJoinedEvent(
                partyId, member.getId(), member.getMemberName(),
                PartyMemberAction.LEFT, LocalDateTime.now()
        );
    }
}
