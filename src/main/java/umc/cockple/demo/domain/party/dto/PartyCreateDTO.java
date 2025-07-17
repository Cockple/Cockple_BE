package umc.cockple.demo.domain.party.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

public class PartyCreateDTO {

    @Builder
    public record Request(

            @NotBlank(message = "모임 이름은 필수입니다.")
            @Size(max = 17, message = "모임 이름은 최대 17글자입니다.")
            String partyName,

            @NotNull(message = "모임 유형은 필수입니다.")
            String partyType,

            @NotNull(message = "여자 급수는 필수 선택 항목입니다.")
            List<String> femaleLevel,

            List<String> maleLevel,

            @NotNull(message = "활동 지역(도/광역시)은 필수 선택 항목입니다.")
            String addr1,

            @NotNull(message = "활동 지역(시/군/구)은 필수 선택 항목입니다.")
            String addr2,

            @NotNull(message = "활동 요일은 필수 선택 항목입니다.")
            List<String> activityDay,

            @NotNull(message = "활동 시간은 필수 선택 항목입니다.")
            String activityTime,

            @NotNull(message = "지정콕 정보는 필수입니다. 없는 경우 없음을 체크해주세요.")
            String designatedCock,

            @NotNull(message = "가입비 정보는 필수입니다. 없는 경우 없음을 체크해주세요.")
            Integer joinPrice,

            @NotNull(message = "월 회비 정보는 필수입니다. 없는 경우 없음을 체크해주세요.")
            Integer price,

            @NotNull(message = "최저 나이 정보는 필수입니다.")
            Integer minAge,

            @NotNull(message = "최대 나이 정보는 필수입니다.")
            Integer maxAge,

            @Size(max = 45, message = "모임 소개는 최대 45글자입니다.")
            String content,

            String imgUrl
    ){
    }

    @Builder
    public record Command(
            String partyName,
            String partyType,
            List<String> femaleLevel,
            List<String> maleLevel,
            List<String> activityDay,
            String activityTime,
            Integer minAge,
            Integer maxAge,
            Integer price,
            Integer joinPrice,
            String designatedCock,
            String content,
            String imgUrl
    ) {
    }

    @Builder
    public record AddrCommand(
            String addr1,
            String addr2
    ) {
    }

    @Builder
    public record Response(
            Long partyId,
            LocalDateTime createdAt
    ) {
    }

}