package umc.cockple.demo.domain.member.dto;

import lombok.Builder;
import lombok.Data;
import umc.cockple.demo.domain.member.domain.ProfileImg;
import umc.cockple.demo.global.enums.Keyword;
import umc.cockple.demo.global.enums.Level;

import java.time.LocalDate;
import java.util.List;

public class MemberRequestDto {

    @Builder
    @Data
    public static class UpdateProfileRequestDto {
        private String memberName;
        private LocalDate birth;
        private Level level;
        private List<Keyword> keywords;
    }


}
