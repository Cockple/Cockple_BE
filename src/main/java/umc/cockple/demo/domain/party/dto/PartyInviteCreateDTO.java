package umc.cockple.demo.domain.party.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

public class PartyInviteCreateDTO {

    @Builder
    @Schema(name = "PartyInviteCreateRequestDTO", description = "모임 초대 생성 요청")
    public record Request(
            @NotNull(message = "초대할 사용자의 ID는 필수입니다.")
            Long userId
    ) {}

    @Builder
    public record Response(
            Long invitationId
    ) {}
}
