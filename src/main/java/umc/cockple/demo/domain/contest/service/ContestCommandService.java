package umc.cockple.demo.domain.contest.service;

import org.springframework.web.multipart.MultipartFile;
import umc.cockple.demo.domain.contest.dto.*;

import java.util.List;

public interface ContestCommandService {
    ContestRecordCreateResponseDTO createContestRecord(
            Long memberId, List<MultipartFile> ContestImage, ContestRecordCreateRequestDTO request
    );

    ContestRecordUpdateResponseDTO updateContestRecord(
            Long memberId, Long contestId, List<MultipartFile> ContestImage, ContestRecordUpdateRequestDTO request
    );

    ContestRecordDeleteResponseDTO deleteContestRecord(
            Long memberId, Long contestId
    );
}
