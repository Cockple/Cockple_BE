package umc.cockple.demo.domain.contest.converter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.contest.dto.ContestRecordCreateCommand;
import umc.cockple.demo.domain.contest.dto.ContestRecordCreateRequestDTO;

@Component
@RequiredArgsConstructor
public class ContestConverter {

    public static ContestRecordCreateCommand toCreateCommand(ContestRecordCreateRequestDTO request, Long memberId) {
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
                .contestImgs(request.contestImgs())
                .build();
    }
}
