package umc.cockple.demo.domain.contest.service;

import org.springframework.web.multipart.MultipartFile;
import umc.cockple.demo.domain.contest.dto.ContestRecordCreateRequestDTO;
import umc.cockple.demo.domain.contest.dto.ContestRecordCreateResponseDTO;

import java.util.List;

public interface ContestCommandService {
    ContestRecordCreateResponseDTO createContestRecord(
            Long memberId, List<MultipartFile> ContestImage, ContestRecordCreateRequestDTO request);
}
