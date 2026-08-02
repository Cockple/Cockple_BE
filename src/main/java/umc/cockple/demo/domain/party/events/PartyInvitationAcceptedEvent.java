package umc.cockple.demo.domain.party.events;

import java.time.LocalDateTime;

public record PartyInvitationAcceptedEvent(
        Long invitationId,
        Long partyId,
        Long inviterId,
        String inviteeNickname,
        String partyName,
        String imageKey,
        LocalDateTime occurredAt
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
                partyName, imageKey, LocalDateTime.now()
        );
    }
}
