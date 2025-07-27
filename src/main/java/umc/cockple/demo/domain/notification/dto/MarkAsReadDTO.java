package umc.cockple.demo.domain.notification.dto;

import umc.cockple.demo.global.enums.NotificationType;

public class MarkAsReadDTO {

    public record Request(
            NotificationType type
    ){}

    public record Response(
            NotificationType type
    ){}
}
