package umc.cockple.demo.domain.notification.service;

import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.notification.domain.NotificationDestination;
import umc.cockple.demo.domain.notification.dto.CreateNotificationRequestDTO;
import umc.cockple.demo.domain.notification.enums.NotificationAction;
import umc.cockple.demo.domain.notification.enums.NotificationResourceType;
import umc.cockple.demo.domain.notification.enums.NotificationTarget;

@Component
public class LegacyNotificationDestinationMapper {

    public NotificationDestination map(CreateNotificationRequestDTO dto) {
        if (dto.target() == NotificationTarget.EXERCISE_DELETE
                || dto.target() == NotificationTarget.PARTY_DELETE) {
            return null;
        }

        if (dto.target() == NotificationTarget.PARTY_INVITE) {
            return new NotificationDestination(
                    NotificationResourceType.PARTY_INVITATION,
                    dto.invitationId(),
                    NotificationAction.RESPOND
            );
        }

        if (dto.target() == NotificationTarget.EXERCISE_MODIFY
                || dto.target() == NotificationTarget.EXERCISE_ATTENDANCE) {
            return new NotificationDestination(
                    NotificationResourceType.EXERCISE,
                    dto.exerciseId(),
                    NotificationAction.VIEW
            );
        }

        return new NotificationDestination(
                NotificationResourceType.PARTY,
                dto.partyId(),
                NotificationAction.VIEW
        );
    }
}
