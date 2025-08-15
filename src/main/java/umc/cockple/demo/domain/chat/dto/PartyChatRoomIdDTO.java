package umc.cockple.demo.domain.chat.dto;

import lombok.Builder;

@Builder
public record PartyChatRoomIdDTO(
        Long roomId
) {}
