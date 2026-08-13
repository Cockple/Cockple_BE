package umc.cockple.demo.domain.party.events;

import java.time.LocalDateTime;
import java.util.UUID;

public record PartyUpdatedEvent(
        Long partyId,
        Long recipientMemberId,
        String partyName,
        String imageKey,
        LocalDateTime occurredAt,
        UUID eventId
) {
    public static PartyUpdatedEvent updated(
            Long partyId,
            Long recipientMemberId,
            String partyName,
            String imageKey
    ) {
        return new PartyUpdatedEvent(
                partyId, recipientMemberId, partyName, imageKey, LocalDateTime.now(), UUID.randomUUID()
        );
    }
}
