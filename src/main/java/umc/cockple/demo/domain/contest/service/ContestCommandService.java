package umc.cockple.demo.domain.contest.service;

import umc.cockple.demo.domain.contest.dto.*;

public interface ContestCommandService {
    ContestRecordCreateDTO.Response createContestRecord(
            Long memberId, ContestRecordCreateDTO.Request request
    );

    ContestRecordUpdateDTO.Response updateContestRecord(
            Long memberId, Long contestId, ContestRecordUpdateDTO.Request request
    );

    ContestRecordDeleteDTO.Response deleteContestRecord(
            Long memberId, Long contestId
    );
}
