package umc.cockple.demo.domain.party.events;

import java.time.LocalDateTime;

public record PartyInfoChangedEvent(
        Long partyId,
        Long recipientMemberId,
        String partyName,
        String imageKey,
        LocalDateTime occurredAt
) {
    public static PartyInfoChangedEvent changed(
            Long partyId,
            Long recipientMemberId,
            String partyName,
            String imageKey
    ) {
        return new PartyInfoChangedEvent(
                partyId, recipientMemberId, partyName, imageKey, LocalDateTime.now()
        );
    }
}
