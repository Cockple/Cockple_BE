package umc.cockple.demo.domain.contest.converter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
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
    public ContestRecordCreateDTO.Command toCreateCommand(ContestRecordCreateDTO.Request request, Long memberId) {
        return ContestRecordCreateDTO.Command.builder()
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
                .contestImgs(request.contestImgs())
                .build();
    }

    public ContestRecordCreateDTO.Response toCreateResponseDTO(Contest contest) {
        return ContestRecordCreateDTO.Response.builder()
                .contestId(contest.getId())
                .createdAt(contest.getCreatedAt())
                .build();
    }

    // 대회 기록 수정
    public ContestRecordUpdateDTO.Response toUpdateResponseDTO(Contest contest) {
        List<ContestImgResponse> imgResponses = contest.getContestImgs().stream()
                .sorted(Comparator.comparing(ContestImg::getImgOrder))
                .map(img -> new ContestImgResponse(img.getId(), img.getImgKey(), img.getImgOrder()))
                .collect(Collectors.toList());

        List<ContestVideoResponse> videoResponses = contest.getContestVideos().stream()
                .sorted(Comparator.comparingInt(ContestVideo::getVideoOrder))
                .map(video -> new ContestVideoResponse(video.getId(), video.getVideoUrl(), video.getVideoOrder()))
                .collect(Collectors.toList());

        return ContestRecordUpdateDTO.Response.builder()
                .contestId(contest.getId())
                .contestImgs(imgResponses)
                .contestVideos(videoResponses)
                .UpdatedAt(contest.getUpdatedAt())
                .build();
    }

    // 대회 기록 삭제
    public ContestRecordDeleteDTO.Response toDeleteResponseDTO(Contest contest) {
        return ContestRecordDeleteDTO.Response.builder()
                .deleteContestId(contest.getId())
                .build();
    }

    // 대회 기록 상세 조회
    public ContestRecordDetailDTO.Response toDetailResponseDTO(Contest contest, List<String> imgUrls, List<String> videoUrls, String content) {
        return ContestRecordDetailDTO.Response.builder()
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

    // 대회 기록 심플 조회
    public static ContestRecordSimpleDTO.Response toSimpleResponseDTO(Contest contest, String medalUrl) {
        return ContestRecordSimpleDTO.Response.builder()
                .contestId(contest.getId())
                .contestName(contest.getContestName())
                .type(contest.getType())
                .level(contest.getLevel())
                .date(contest.getDate())
                .medalImgUrl(medalUrl)
                .build();
    }

    // 대회 메달 개수 조회
    public ContestMedalSummaryDTO.Response toMedalSummaryResponseDTO(int gold, int silver, int bronze) {
        return ContestMedalSummaryDTO.Response.builder()
                .myMedalTotal(gold + silver + bronze)
                .goldCount(gold)
                .silverCount(silver)
                .bronzeCount(bronze)
                .build();
    }
}
