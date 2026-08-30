package umc.cockple.demo.domain.party.events;

import java.time.LocalDateTime;
import java.util.UUID;

public record PartyInvitationAcceptedEvent(
        Long invitationId,
        Long partyId,
        Long inviterId,
        String inviteeNickname,
        String partyName,
        String imageKey,
        LocalDateTime occurredAt,
        UUID eventId
) {
    public static PartyInvitationAcceptedEvent accepted(
            Long invitationId,
            Long partyId,
            Long inviterId,
            String inviteeNickname,
            String partyName,
            String imageKey
    ) {
        return new PartyInvitationAcceptedEvent(
                invitationId, partyId, inviterId, inviteeNickname,
                partyName, imageKey, LocalDateTime.now(), UUID.randomUUID()
        );
    }
}
