package umc.cockple.demo.domain.party.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

public class PartyKeywordDTO {
    @Builder
    @Schema(name = "PartyKeywordRequestDTO", description = "모임 키워드 추가/삭제 요청")
    public record Request(
            @NotBlank(message = "키워드는 필수입니다.")
            String keyword
    ){}
}
