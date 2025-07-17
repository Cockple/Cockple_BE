package umc.cockple.demo.domain.member.dto;

import lombok.Builder;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;

import java.time.LocalDate;

@Builder
public record GetProfileResponseDTO(
        String memberName,
        LocalDate birth,
        Gender gender,
        Level level,
        String profileImgUrl,
        Integer myPartyCnt,
        Integer myGoldMedalCnt,
        Integer mySilverMedalCnt,
        Integer myBronzeMedalCnt

) {
}
