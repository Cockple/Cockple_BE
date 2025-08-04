package umc.cockple.demo.domain.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Keyword;
import umc.cockple.demo.global.enums.Level;

import java.time.LocalDate;
import java.util.List;

@Builder
public record MemberDetailInfoRequestDTO(

        @NotBlank(message = "회원명은 필수입니다.")
        String memberName,

        @NotNull(message = "성별은 필수입니다.")
        Gender gender,

        @NotNull(message = "생년월일은 필수입니다.")
        LocalDate birth,

        @NotNull(message = "급수는 필수입니다.")
        Level level,

        String imgKey,

        List<Keyword> keywords
) {
}
