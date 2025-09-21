package umc.cockple.demo.domain.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Keyword;
import umc.cockple.demo.global.enums.Level;

import java.time.LocalDate;
import java.util.List;

@Builder
public record GetMyProfileResponseDTO(

        String memberName,
        LocalDate birth,
        Gender gender,
        Level level,
        List<Keyword> keywords,
        String addr3,
        String streetAddr,
        String buildingName,
        Double latitude,
        Double longitude,
        String profileImgUrl,
        Integer myPartyCnt,
        Integer myExerciseCnt,
        Integer myGoldMedalCnt,
        Integer mySilverMedalCnt,
        Integer myBronzeMedalCnt
) {
}
