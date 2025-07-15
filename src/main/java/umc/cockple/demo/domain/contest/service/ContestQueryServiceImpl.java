package umc.cockple.demo.domain.contest.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import umc.cockple.demo.domain.contest.converter.ContestConverter;
import umc.cockple.demo.domain.contest.domain.Contest;
import umc.cockple.demo.domain.contest.dto.ContestMedalSummaryResponseDTO;
import umc.cockple.demo.domain.contest.dto.ContestRecordDetailResponseDTO;
import umc.cockple.demo.domain.contest.dto.ContestRecordSimpleResponseDTO;
import umc.cockple.demo.domain.contest.exception.ContestErrorCode;
import umc.cockple.demo.domain.contest.exception.ContestException;
import umc.cockple.demo.domain.contest.repository.ContestRepository;
import umc.cockple.demo.global.enums.MedalType;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ContestQueryServiceImpl implements ContestQueryService {

    private final ContestRepository contestRepository;
    private final ContestConverter contestConverter;

    // 내 대회 기록 상세 조회
    @Override
    public ContestRecordDetailResponseDTO getMyContestRecordDetail(Long memberId, Long contestId) {

        log.info("[내 대회 기록 상세 조회 시작] - memberId: {}, contestId: {}", memberId, contestId);

        // 1. 대회 조회 + 본인 확인
        Contest contest = contestRepository.findByIdAndMember_Id(contestId, memberId)
                .orElseThrow(() -> new ContestException(ContestErrorCode.CONTEST_NOT_FOUND));

        return contestConverter.toDetailResponseDTO(contest);
    }

    // 내 대회 기록 리스트 조회 (전체, 미입상)
    @Override
    public List<ContestRecordSimpleResponseDTO> getMyContestRecordsByMedalType(Long memberId, MedalType medalType) {

        // 1. 대회 전체 조회
        List<Contest> contests = contestRepository.findAllByMember_Id(memberId);

        // 2. 미입상 요청이면 필터링
        if (medalType == MedalType.NONE) {
            List<Contest> noneMedalContests = contests.stream()
                    .filter(c -> c.getMedalType() == MedalType.NONE)
                    .toList();
            return contestConverter.toSimpleDTOList(noneMedalContests);
        }

        return contestConverter.toSimpleDTOList(contests);
    }

    // 내 메달 개수 조회
    @Override
    public ContestMedalSummaryResponseDTO getMyMedalSummary(Long memberId) {

        int gold = contestRepository.countGoldMedalsByMemberId(memberId);
        int silver = contestRepository.countSilverMedalsByMemberId(memberId);
        int bronze = contestRepository.countBronzeMedalsByMemberId(memberId);

        return contestConverter.toMedalSummaryResponseDTO(gold, silver, bronze);
    }

}
