package umc.cockple.demo.domain.party.events;

import java.time.LocalDateTime;
import java.util.UUID;

public record PartyJoinRequestApprovedEvent(
        Long partyId,
        Long recipientMemberId,
        String partyName,
        String imageKey,
        LocalDateTime occurredAt,
        UUID eventId
) {
    public static PartyJoinRequestApprovedEvent approved(
            Long partyId,
            Long recipientMemberId,
            String partyName,
            String imageKey
    ) {
        return new PartyJoinRequestApprovedEvent(
                partyId, recipientMemberId, partyName, imageKey, LocalDateTime.now(), UUID.randomUUID()
        );
    }
}
