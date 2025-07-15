package umc.cockple.demo.domain.contest.converter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import umc.cockple.demo.domain.contest.domain.Contest;
import umc.cockple.demo.domain.contest.domain.ContestImg;
import umc.cockple.demo.domain.contest.domain.ContestVideo;
import umc.cockple.demo.domain.contest.dto.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ContestConverter {

    // 대회 기록 등록
    public ContestRecordCreateCommand toCreateCommand(ContestRecordCreateRequestDTO request, Long memberId, List<MultipartFile> contestImgs) {
        return ContestRecordCreateCommand.builder()
                .memberId(memberId)
                .contestName(request.contestName())
                .date(request.date())
                .medalType(request.medalType())
                .type(request.type())
                .level(request.level())
                .content(request.content())
                .contentIsOpen(request.contentIsOpen())
                .videoIsOpen(request.videoIsOpen())
                .contestVideos(request.contestVideos())
                .contestImgs(contestImgs)
                .build();
    }

    public ContestRecordCreateResponseDTO toCreateResponseDTO(Contest contest) {
        return ContestRecordCreateResponseDTO.builder()
                .contestId(contest.getId())
                .createdAt(contest.getCreatedAt())
                .build();
    }

    // 대회 기록 수정
    public ContestRecordUpdateResponseDTO toUpdateResponseDTO(Contest contest) {
        return ContestRecordUpdateResponseDTO.builder()
                .contestId(contest.getId())
                .UpdatedAt(contest.getUpdatedAt())
                .build();
    }

    // 대회 기록 삭제
    public ContestRecordDeleteResponseDTO toDeleteResponseDTO(Contest contest) {
        return ContestRecordDeleteResponseDTO.builder()
                .deleteContestId(contest.getId())
                .build();
    }

    // 대회 기록 상세 조회
    public ContestRecordDetailResponseDTO toDetailResponseDTO(Contest contest, Boolean isOwner) {
        List<String> imgUrls = contest.getContestImgs().stream()
                .sorted(Comparator.comparing(ContestImg::getImgOrder))
                .map(ContestImg::getImgUrl)
                .collect(Collectors.toList());

        // video 링크 공개여부 처리
        List<String> videoUrls = (contest.getVideoIsOpen() || isOwner)
                ? contest.getContestVideos().stream()
                .sorted(Comparator.comparingInt(ContestVideo::getVideoOrder))
                .map(ContestVideo::getVideoUrl)
                .collect(Collectors.toList())
                : List.of();

        // 기록 공개여부 처리
        String content = (contest.getContentIsOpen() || isOwner)
                ? contest.getContent()
                : "";

        return ContestRecordDetailResponseDTO.builder()
                .contestId(contest.getId())
                .contestName(contest.getContestName())
                .date(contest.getDate())
                .medalType(contest.getMedalType())
                .type(contest.getType())
                .level(contest.getLevel())
                .contentIsOpen(contest.getContentIsOpen())
                .content(content)
                .contestImgUrls(imgUrls)
                .videoIsOpen(contest.getVideoIsOpen())
                .contestVideoUrls(videoUrls)
                .build();
    }

    // 내 대회 기록 심플 조회
    public static ContestRecordSimpleResponseDTO toSimpleResponseDTO(Contest contest) {
        return ContestRecordSimpleResponseDTO.builder()
                .contestId(contest.getId())
                .contestName(contest.getContestName())
                .type(contest.getType())
                .level(contest.getLevel())
                .date(contest.getDate())
                .medalImgUrl(getMedalImgUrl(contest))
                .build();
    }

    // 내 대회 기록 리스트 변환
    public List<ContestRecordSimpleResponseDTO> toSimpleDTOList(List<Contest> contests) {
        return contests.stream()
                .map(ContestConverter::toSimpleResponseDTO)
                .collect(Collectors.toList());
    }

    // todo: 임시 Url
    public static String getMedalImgUrl(Contest contest) {
        return switch (contest.getMedalType()) {
            case GOLD -> "/images/medal/gold.png";
            case SILVER ->  "/images/medal/silver.png";
            case BRONZE -> "/images/medal/bronze.png";
            case NONE -> "/images/medal/none.png";
        };
    }

    // 내 대회 메달 개수 조회
    public ContestMedalSummaryResponseDTO toMedalSummaryResponseDTO(int gold, int silver, int bronze) {
        return ContestMedalSummaryResponseDTO.builder()
                .myMedalTotal(gold + silver + bronze)
                .goldCount(gold)
                .silverCount(silver)
                .bronzeCount(bronze)
                .build();
    }
}
