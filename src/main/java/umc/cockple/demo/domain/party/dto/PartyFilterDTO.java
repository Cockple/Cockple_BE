package umc.cockple.demo.domain.party.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

public class PartyFilterDTO {
    @Getter
    @Builder
    @Schema(name = "PartyFilterRequestDTO", description = "모임 추천 조회 요청")
    public record Request(
            String addr1,
            String addr2,
            List<String> level,
            List<String> partyType,
            List<String> activityDay,
            List<String> activityTime,
            List<String> keyword
    ) {}
}
