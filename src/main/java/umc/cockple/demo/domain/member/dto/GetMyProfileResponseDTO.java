package umc.cockple.demo.domain.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;

import java.time.LocalDate;

@Builder
public record GetMyProfileResponseDTO(

        String memberName,
        LocalDate birth,
        Gender gender,
        Level level,
        String addr3,
        Float latitude,
        Float longitude,
        String profileImgUrl,
        Integer myPartyCnt,
        Integer myExerciseCnt,
        Integer myGoldMedalCnt,
        Integer mySilverMedalCnt,
        Integer myBronzeMedalCnt
) {
}
