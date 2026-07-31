package umc.cockple.demo.domain.notification.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record NotificationV2ListResponseDTO(
        List<NotificationV2ResponseDTO> notifications,
        Boolean hasNext,
        Long nextCursor,
        int totalElements
) {
}
