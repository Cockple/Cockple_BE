package umc.cockple.demo.domain.party.dto;

import lombok.Builder;

public class PartyMemberSuggestionDTO {
    @Builder
    public record Response(
            Long userId,
            String nickname,
            String profileImageUrl,
            String gender,
            String level
    ) {}
}
