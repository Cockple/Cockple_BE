package umc.cockple.demo.domain.contest.service;

import umc.cockple.demo.domain.contest.dto.ContestRecordDetailResponseDTO;
import umc.cockple.demo.domain.contest.dto.ContestRecordSimpleResponseDTO;

import java.util.List;

public interface ContestQueryService {
    ContestRecordDetailResponseDTO getMyContestRecordDetail(Long memberId, Long contestId);

    List<ContestRecordSimpleResponseDTO> getMyContestRecords(Long memberId);

    List<ContestRecordSimpleResponseDTO> getMyNonMedalRecords(Long memberId);  // ✅ 추가

}
