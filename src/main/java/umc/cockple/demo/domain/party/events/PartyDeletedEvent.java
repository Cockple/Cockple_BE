package umc.cockple.demo.domain.party.events;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

public record PartyDeletedEvent(
        Long partyId,
        Long deletedByMemberId,
        List<Long> recipientMemberIds,
        String partyName,
        String imageKey,
        LocalDateTime occurredAt,
        UUID eventId
) {
    public static PartyDeletedEvent deleted(
            Long partyId,
            Long deletedByMemberId,
            List<Long> recipientMemberIds,
            String partyName,
            String imageKey
    ) {
        return new PartyDeletedEvent(
                partyId,
                deletedByMemberId,
                List.copyOf(recipientMemberIds),
                partyName,
                imageKey,
                LocalDateTime.now(),
                UUID.randomUUID()
        );
    }
}
