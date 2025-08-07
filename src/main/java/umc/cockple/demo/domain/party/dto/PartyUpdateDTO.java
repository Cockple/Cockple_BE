package umc.cockple.demo.domain.party.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

import java.util.List;

public class PartyUpdateDTO {
    @Builder
    @Schema(name = "PartyUpdateRequestDTO", description = "모임 정보 수정 요청")
    public record Request(
            @NotEmpty(message = "활동 요일은 필수 선택 항목입니다.")
            List<String> activityDay,
            @NotBlank(message = "활동 시간은 필수 선택 항목입니다.")
            String activityTime,
            String designatedCock,
            Integer joinPrice,
            Integer price,
            String content,
            String imgKey,
            List<String> keyword
    ){}
}
