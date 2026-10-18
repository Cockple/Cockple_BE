package umc.cockple.demo.domain.party.events;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

public record PartyUpdatedEvent(
        Long partyId,
        List<Long> recipientMemberIds,
        String partyName,
        String imageKey,
        LocalDateTime occurredAt,
        UUID eventId
) {
    public static PartyUpdatedEvent updated(
            Long partyId,
            List<Long> recipientMemberIds,
            String partyName,
            String imageKey
    ) {
        return new PartyUpdatedEvent(
                partyId, List.copyOf(recipientMemberIds), partyName, imageKey, LocalDateTime.now(), UUID.randomUUID()
        );
    }
}
