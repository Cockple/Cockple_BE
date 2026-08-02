package umc.cockple.demo.domain.party.events;

import java.time.LocalDateTime;

public record PartyJoinRequestApprovedEvent(
        Long partyId,
        Long recipientMemberId,
        String partyName,
        String imageKey,
        LocalDateTime occurredAt
) {
    public static PartyJoinRequestApprovedEvent approved(
            Long partyId,
            Long recipientMemberId,
            String partyName,
            String imageKey
    ) {
        return new PartyJoinRequestApprovedEvent(
                partyId, recipientMemberId, partyName, imageKey, LocalDateTime.now()
        );
    }
}
