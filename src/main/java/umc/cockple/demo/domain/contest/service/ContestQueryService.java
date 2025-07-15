package umc.cockple.demo.domain.contest.service;

import umc.cockple.demo.domain.contest.dto.ContestRecordDetailResponseDTO;
import umc.cockple.demo.domain.contest.dto.ContestRecordSimpleResponseDTO;
import umc.cockple.demo.global.enums.MedalType;

import java.util.List;

public interface ContestQueryService {
    ContestRecordDetailResponseDTO getMyContestRecordDetail(Long memberId, Long contestId);

    List<ContestRecordSimpleResponseDTO> getMyContestRecordsByMedalType(Long memberId, MedalType medalType);

}
