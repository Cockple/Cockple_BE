package umc.cockple.demo.domain.party.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

public class PartyUpdateDTO {
    @Builder
    @Schema(name = "PartyUpdateRequestDTO", description = "모임 정보 수정 요청")
    public record Request(
            List<String> activityDay,
            String activityTime,
            String designatedCock,
            Integer joinPrice,
            Integer price,
            String content,
            String imgKey,
            List<String> keyword
    ){}
}
