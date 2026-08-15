package umc.cockple.demo.domain.party.events;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PartyRoleChangedEvent(
        Long partyId,
        List<Long> recipientMemberIds,
        String partyName,
        String imageKey,
        String subjectNickname,
        RoleChangeAction action,
        LocalDateTime occurredAt,
        UUID eventId
) {
    public PartyRoleChangedEvent {
        recipientMemberIds = List.copyOf(recipientMemberIds);
    }

    public static PartyRoleChangedEvent changed(
            Long partyId,
            List<Long> recipientMemberIds,
            String partyName,
            String imageKey,
            String subjectNickname,
            RoleChangeAction action
    ) {
        return new PartyRoleChangedEvent(
                partyId, recipientMemberIds, partyName, imageKey,
                subjectNickname, action, LocalDateTime.now(), UUID.randomUUID()
        );
    }

    public enum RoleChangeAction {
        SUBOWNER_ASSIGNED,
        SUBOWNER_RELEASED
    }
}
