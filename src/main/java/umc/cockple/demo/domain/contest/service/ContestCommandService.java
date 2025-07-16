package umc.cockple.demo.domain.contest.service;

import org.springframework.web.multipart.MultipartFile;
import umc.cockple.demo.domain.contest.dto.*;

import java.util.List;

public interface ContestCommandService {
    ContestRecordCreateDTO.Response createContestRecord(
            Long memberId, List<MultipartFile> ContestImage, ContestRecordCreateDTO.Request request
    );

    ContestRecordUpdateDTO.Response updateContestRecord(
            Long memberId, Long contestId, List<MultipartFile> ContestImage, ContestRecordUpdateDTO.Request request
    );

    ContestRecordDeleteDTO.Response deleteContestRecord(
            Long memberId, Long contestId
    );
}
