package umc.cockple.demo.domain.contest.dto;

import lombok.Builder;
import org.springframework.web.multipart.MultipartFile;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.global.enums.MedalType;
import umc.cockple.demo.global.enums.ParticipationType;

import java.time.LocalDate;
import java.util.List;

@Builder
public record ContestRecordUpdateCommand(
        Long memberId,
        String contestName,
        LocalDate date,
        MedalType medalType,
        ParticipationType type,
        Level level,
        String content,
        Boolean contentIsOpen,
        Boolean videoIsOpen,
        List<String> contestVideos,
        List<Long> contestVideoIdsToDelete,
        List<MultipartFile> contestImgs,
        List<String> contestImgsToDelete
) {

}
