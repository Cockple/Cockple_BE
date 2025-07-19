package umc.cockple.demo.domain.party.dto;

import lombok.Builder;

import java.util.List;

public class PartyDTO {
    @Builder
    public record Response(
            Long partyId,
            String partyName,
            String addr1,
            String addr2,
            List<String> femaleLevel,
            List<String> maleLevel,
            String nextExerciseInfo,
            Integer totalExerciseCount,
            String partyImgUrl
    ) {}
}
