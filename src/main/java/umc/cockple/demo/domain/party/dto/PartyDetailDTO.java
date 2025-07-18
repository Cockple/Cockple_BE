package umc.cockple.demo.domain.party.dto;

import lombok.Builder;

import java.util.List;

public class PartyDetailDTO {

    @Builder
    public record Response(
            Long partyId,
            String partyName,
            MemberInfo memberInfo,
            AddrInfo partyAddr,
            List<String> activityDays,
            String activityTime,
            LevelInfo partyLevel,
            Integer minAge,
            Integer maxAge,
            Integer price,
            Integer joinPrice,
            String designatedCock,
            String content,
            List<String> keywords,
            String partyImgUrl
    ) {}

    @Builder
    public record MemberInfo(
            String status,
            String role
    ) {}

    @Builder
    public record AddrInfo(
            String addr1,
            String addr2
    ) {}

    @Builder
    public record LevelInfo(
            String femaleLevel,
            String maleLevel
    ) {}
}