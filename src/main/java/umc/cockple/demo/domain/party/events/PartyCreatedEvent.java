package umc.cockple.demo.domain.party.events;

import java.time.LocalDateTime;

public record PartyCreatedEvent(
        Long partyId,
        Long ownerId,
        LocalDateTime occurredAt
) {
    public static PartyCreatedEvent created(Long partyId, Long ownerId) {
        return new PartyCreatedEvent(partyId, ownerId, LocalDateTime.now());
    }
}
