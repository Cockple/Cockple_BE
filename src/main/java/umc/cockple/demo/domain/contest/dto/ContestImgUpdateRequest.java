package umc.cockple.demo.domain.contest.dto;

public record ContestImgUpdateRequest(
        Long id,           // 기존 항목이면 id, 신규면 null
        String imgKey,
        int imgOrder
) {
}
