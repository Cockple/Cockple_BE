package umc.cockple.demo.domain.member.dto;

import lombok.Builder;

@Builder
public record OnboardingStatusResponseDTO(
        boolean needsOnboarding
) {
}
