package umc.cockple.demo.domain.party.events;

import java.time.LocalDateTime;

public record PartyDeletedEvent(
        Long partyId,
        Long deletedByMemberId,
        String partyName,
        String imageKey,
        LocalDateTime occurredAt
) {
    public static PartyDeletedEvent deleted(Long partyId, Long deletedByMemberId) {
        return new PartyDeletedEvent(
                partyId,
                deletedByMemberId,
                null,
                null,
                LocalDateTime.now()
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
                LocalDateTime.now()
        );
    }
}
