package umc.cockple.demo.domain.party.dto;

import lombok.Builder;

import java.util.List;

public class PartyDetailDTO {

    @Builder
    public record Response(
            Long partyId,
            Long ownerId,
            String partyName,
            String memberStatus,
            String memberRole,
            Boolean hasPendingJoinRequest,
            String addr1,
            String addr2,
            List<String> activityDays,
            String activityTime,
            List<String> femaleLevel,
            List<String> maleLevel,
            Integer minBirthYear,
            Integer maxBirthYear,
            Integer price,
            Integer joinPrice,
            String designatedCock,
            String content,
            List<String> keywords,
            String partyImgUrl
    ) {}
}