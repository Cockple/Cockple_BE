package umc.cockple.demo.domain.contest.dto;

public record ContestVideoResponse(
        Long id,
        String videoUrl,
        int videoOrder
) {
}
