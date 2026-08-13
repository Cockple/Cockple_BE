package umc.cockple.demo.domain.party.events;

import java.time.LocalDateTime;
import java.util.UUID;

public record PartyInvitationCreatedEvent(
        Long invitationId,
        Long partyId,
        Long inviteeId,
        String partyName,
        String imageKey,
        LocalDateTime occurredAt,
        UUID eventId
) {
    public static PartyInvitationCreatedEvent created(
            Long invitationId,
            Long partyId,
            Long inviteeId,
            String partyName,
            String imageKey
    ) {
        return new PartyInvitationCreatedEvent(
                invitationId, partyId, inviteeId, partyName, imageKey, LocalDateTime.now(), UUID.randomUUID()
        );
    }
}
