package umc.cockple.demo.domain.member.dto.request;

import umc.cockple.demo.global.enums.Keyword;
import umc.cockple.demo.global.enums.Level;

import java.time.LocalDate;
import java.util.List;

public record UpdateProfileRequestDTO(
        String memberName,
        LocalDate birth,
        Level level,
        List<Keyword> keywords

) {
}
