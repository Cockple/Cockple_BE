package umc.cockple.demo.domain.notification.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record NotificationListResponseDTO(
        List<AllNotificationsResponseDTO> notifications,
        Boolean hasNext,
        Long nextCursor,
        int totalElements
) {
}
