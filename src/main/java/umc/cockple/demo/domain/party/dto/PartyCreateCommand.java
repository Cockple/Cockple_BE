package umc.cockple.demo.domain.party.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record PartyCreateCommand(
        String partyName,
        String partyType,
        List<String> femaleLevel,
        List<String> maleLevel,
        List<String> activityDay,
        String activityTime,
        Integer minAge,
        Integer maxAge,
        Integer price,
        Integer joinPrice,
        String designatedCock,
        String content,
        String imgUrl
) {
}