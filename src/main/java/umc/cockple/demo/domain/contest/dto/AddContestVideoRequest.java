package umc.cockple.demo.domain.contest.dto;

public record AddContestVideoRequest(
        String videoKey,
        int videoOrder
) {
}
