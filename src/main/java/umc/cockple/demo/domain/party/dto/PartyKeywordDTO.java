package umc.cockple.demo.domain.party.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;

public class PartyKeywordDTO {
    @Builder
    @Schema(name = "PartyKeywordRequestDTO", description = "모임 키워드 추가 요청")
    public record Request(
            @NotNull(message = "키워드 목록은 필수입니다.")
            List<String> keywords
    ){}
}
