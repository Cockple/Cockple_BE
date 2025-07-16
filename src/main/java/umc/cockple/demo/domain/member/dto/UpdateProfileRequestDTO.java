package umc.cockple.demo.domain.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import umc.cockple.demo.global.enums.Keyword;
import umc.cockple.demo.global.enums.Level;

import java.time.LocalDate;
import java.util.List;

public record UpdateProfileRequestDTO(
        @NotBlank(message = "회원명은 필수입니다.")
        String memberName,
        @NotNull(message = "생년월일은 필수입니다.")
        LocalDate birth,
        @NotNull(message = "급수는 필수입니다.")
        Level level,
        @NotNull(message = "키워드는 필수입니다.")
        List<Keyword> keywords,
        String imgUrl

) {
}
