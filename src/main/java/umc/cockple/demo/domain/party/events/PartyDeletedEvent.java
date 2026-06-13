package umc.cockple.demo.domain.party.events;

import java.time.LocalDateTime;

public record PartyDeletedEvent(
        Long partyId,
        Long deletedByMemberId,
        LocalDateTime occurredAt
) {
    public static PartyDeletedEvent deleted(Long partyId, Long deletedByMemberId) {
        return new PartyDeletedEvent(
                partyId,
                deletedByMemberId,
                LocalDateTime.now()
        );
    }
}
