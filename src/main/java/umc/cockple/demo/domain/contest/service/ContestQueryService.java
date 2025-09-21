package umc.cockple.demo.domain.contest.service;

import umc.cockple.demo.domain.contest.dto.*;
import umc.cockple.demo.domain.contest.enums.MedalType;

import java.util.List;

public interface ContestQueryService {
    ContestRecordDetailDTO.Response getContestRecordDetail(Long loginMemberId, Long memberId, Long contestId);

    List<ContestRecordSimpleDTO.Response> getMyContestRecordsByMedalType(Long memberId, MedalType medalType);

    ContestMedalSummaryDTO.Response getMyMedalSummary(Long memberId);

}
