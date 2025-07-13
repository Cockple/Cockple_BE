package umc.cockple.demo.domain.contest.converter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import umc.cockple.demo.domain.contest.domain.Contest;
import umc.cockple.demo.domain.contest.dto.*;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ContestConverter {

    // 대회 기록 등록
    public ContestRecordCreateCommand toCreateCommand(ContestRecordCreateRequestDTO request, Long memberId, List<MultipartFile> contestImgs) {
        return ContestRecordCreateCommand.builder()
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
                .contestImgs(contestImgs)
                .build();
    }

    public ContestRecordCreateResponseDTO toCreateResponseDTO(Contest contest) {
        return ContestRecordCreateResponseDTO.builder()
                .contestId(contest.getId())
                .createdAt(contest.getCreatedAt())
                .build();
    }

    // 대회 기록 수정
    public ContestRecordUpdateResponseDTO toUpdateResponseDTO(Contest contest) {
        return ContestRecordUpdateResponseDTO.builder()
                .contestId(contest.getId())
                .UpdatedAt(contest.getUpdatedAt())
                .build();
    }
}
