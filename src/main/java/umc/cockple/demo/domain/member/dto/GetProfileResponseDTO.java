package umc.cockple.demo.domain.member.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import umc.cockple.demo.domain.member.domain.MemberKeyword;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Keyword;
import umc.cockple.demo.global.enums.Level;

import java.time.LocalDate;
import java.util.List;

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
