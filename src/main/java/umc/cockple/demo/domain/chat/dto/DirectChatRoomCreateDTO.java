package umc.cockple.demo.domain.chat.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

public class DirectChatRoomCreateDTO {

    @Builder
    public record Response(
            Long chatRoomId,
            String displayName,
            LocalDateTime createdAt,
            List<MemberInfo> members
    ) {
    }

    @Builder
    public record MemberInfo(
            Long memberId,
            String memberName
    ) {
    }
}
