package umc.cockple.demo.domain.contest.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import umc.cockple.demo.domain.contest.converter.ContestConverter;
import umc.cockple.demo.domain.contest.domain.Contest;
import umc.cockple.demo.domain.contest.domain.ContestImg;
import umc.cockple.demo.domain.contest.domain.ContestVideo;
import umc.cockple.demo.domain.contest.dto.*;
import umc.cockple.demo.domain.contest.exception.ContestErrorCode;
import umc.cockple.demo.domain.contest.exception.ContestException;
import umc.cockple.demo.domain.contest.repository.ContestRepository;
import umc.cockple.demo.domain.contest.enums.MedalType;
import umc.cockple.demo.domain.image.service.ImageService;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ContestQueryServiceImpl implements ContestQueryService {

    private final ContestRepository contestRepository;
    private final ContestConverter contestConverter;
    private final ImageService imageService;

    // 대회 기록 상세 조회
    @Override
    public ContestRecordDetailDTO.Response getContestRecordDetail(Long loginMemberId, Long memberId, Long contestId) {

        log.info("[대회 기록 상세조회 시작] - 요청자: {}, 기록주인: {}, contestId: {}", loginMemberId, memberId, contestId);

        Contest contest = contestRepository.findByIdAndMember_Id(contestId, memberId)
                .orElseThrow(() -> new ContestException(ContestErrorCode.CONTEST_NOT_FOUND));

        boolean isOwner = loginMemberId.equals(memberId);

        List<String> imgUrls = getImageUrls(contest);
        List<String> videoUrls = getVideoUrls(contest, isOwner);
        String content = getContent(contest, isOwner);

        log.info("대회 기록 상세조회 완료 - contestId: {}", contestId);

        return contestConverter.toDetailResponseDTO(contest, imgUrls, videoUrls, content);
    }

    // 대회 기록 리스트 조회 (전체, 미입상)
    @Override
    public List<ContestRecordSimpleDTO.Response> getMyContestRecordsByMedalType(Long memberId, MedalType medalType) {

        log.info("[대회 기록 리스트 조회 시작] - memberId: {}", memberId);

        // 1. 대회 전체 조회
        List<Contest> contests = contestRepository.findAllByMember_Id(memberId);

        // 2. 미입상 요청이면 필터링
        if (medalType == MedalType.NONE) {
            contests = contests.stream()
                    .filter(c -> c.getMedalType() == MedalType.NONE)
                    .toList();

            log.info("[미입상] 대회 기록 조회 완료 - memberId: {}", memberId);
        } else {
            log.info("[전체] 대회 기록 조회 완료 - memberId: {}", memberId);
        }

        // 3. medalUrl 생성 + DTO 변환
        return contests.stream()
                .map(contest -> {
                    String medalUrl = getMedalImgUrl(contest); // S3 URL 생성
                    return contestConverter.toSimpleResponseDTO(contest, medalUrl);
                })
                .toList();    }

    // 메달 개수 조회
    @Override
    public ContestMedalSummaryDTO.Response getMyMedalSummary(Long memberId) {

        log.info("[메달 개수 조회 시작] - memberId: {}", memberId);

        int gold = contestRepository.countGoldMedalsByMemberId(memberId);
        int silver = contestRepository.countSilverMedalsByMemberId(memberId);
        int bronze = contestRepository.countBronzeMedalsByMemberId(memberId);

        log.info("[메달 조회 완료] - memberId: {}", memberId);

        return contestConverter.toMedalSummaryResponseDTO(gold, silver, bronze);
    }

    // 이미지 URL 리스트 반환
    private List<String> getImageUrls(Contest contest) {
        return contest.getContestImgs().stream()
                .sorted(Comparator.comparing(ContestImg::getImgOrder))
                .map(img -> imageService.getUrlFromKey(img.getImgKey()))
                .collect(Collectors.toList());
    }

    // 영상 URL 리스트 (공개 여부에 따라)
    private List<String> getVideoUrls(Contest contest, boolean isOwner) {
        if (contest.getVideoIsOpen() || isOwner) {
            return contest.getContestVideos().stream()
                    .sorted(Comparator.comparingInt(ContestVideo::getVideoOrder))
                    .map(ContestVideo::getVideoUrl)
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    // 내용 (공개 여부에 따라)
    private String getContent(Contest contest, boolean isOwner) {
        return (contest.getContentIsOpen() || isOwner)
                ? contest.getContent()
                : "";
    }

    public String getMedalImgUrl(Contest contest) {
        String baseKey = "contest/";
        String key = switch (contest.getMedalType()) {
            case GOLD   -> baseKey + "b0ac9ac7-169a-40de-aeb3-a8572bc91506.svg";
            case SILVER -> baseKey + "c0a3b94c-bf46-4aa0-934f-0c427475bc0b.svg";
            case BRONZE -> baseKey + "3f9778a5-479a-44cf-bfb0-bea187a839c5.svg";
            case NONE   -> baseKey + "84e4dd20-7989-4871-954b-7363213b941e.svg";
        };
        return imageService.getUrlFromKey(key);
    }
}
