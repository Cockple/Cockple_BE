package umc.cockple.demo.domain.party.events;

import java.time.LocalDateTime;
import java.util.UUID;

public record PartyDeletedEvent(
        Long partyId,
        Long deletedByMemberId,
        String partyName,
        String imageKey,
        LocalDateTime occurredAt,
        UUID eventId
) {
    public static PartyDeletedEvent deleted(Long partyId, Long deletedByMemberId) {
        return new PartyDeletedEvent(
                partyId,
                deletedByMemberId,
                null,
                null,
                LocalDateTime.now(),
                UUID.randomUUID()
        );
    }

    public static PartyDeletedEvent deleted(
            Long partyId,
            Long deletedByMemberId,
            String partyName,
            String imageKey
    ) {
        return new PartyDeletedEvent(
                partyId,
                deletedByMemberId,
                partyName,
                imageKey,
                LocalDateTime.now(),
                UUID.randomUUID()
        );
    }
}
