package umc.cockple.demo.domain.contest.dto;


import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import org.springframework.web.multipart.MultipartFile;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.global.enums.MedalType;
import umc.cockple.demo.global.enums.ParticipationType;

import java.time.LocalDate;
import java.util.List;

public record ContestRecordCreateRequestDTO (

    @NotBlank(message = "대회 이름은 필수입니다.")
    String contestName,

    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate date,

    MedalType medalType,

    @NotNull(message = "참여 형태는 필수입니다.")
    ParticipationType type,

    @NotNull(message = "참여 급수는 필수입니다.")
    Level level,

    String content,

    Boolean contentIsOpen,

    Boolean videoIsOpen,

    List<String> contestVideos,

    List<MultipartFile> contestImgs

 ){}
