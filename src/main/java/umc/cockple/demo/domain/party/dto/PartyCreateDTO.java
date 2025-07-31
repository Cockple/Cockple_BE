package umc.cockple.demo.domain.party.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import umc.cockple.demo.domain.party.exception.PartyErrorCode;
import umc.cockple.demo.domain.party.exception.PartyException;
import umc.cockple.demo.domain.party.enums.ActiveDay;
import umc.cockple.demo.domain.party.enums.ActivityTime;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.domain.party.enums.ParticipationType;
import umc.cockple.demo.global.exception.GeneralException;

import java.time.LocalDateTime;
import java.util.List;

public class PartyCreateDTO {

    @Builder
    @Schema(name = "PartyCreateRequestDTO", description = "모임 생성 요청")
    public record Request(

            @NotBlank(message = "모임 이름은 필수입니다.")
            @Size(max = 17, message = "모임 이름은 최대 17글자입니다.")
            String partyName,

            @NotBlank(message = "모임 유형은 필수입니다.")
            String partyType,

            @NotEmpty(message = "여자 급수는 필수 선택 항목입니다.")
            List<String> femaleLevel,

            List<String> maleLevel,

            @NotBlank(message = "활동 지역(도/광역시)은 필수 선택 항목입니다.")
            String addr1,

            @NotBlank(message = "활동 지역(시/군/구)은 필수 선택 항목입니다.")
            String addr2,

            @NotEmpty(message = "활동 요일은 필수 선택 항목입니다.")
            List<String> activityDay,

            @NotBlank(message = "활동 시간은 필수 선택 항목입니다.")
            String activityTime,

            @NotNull(message = "지정콕 정보는 필수입니다. 없는 경우 없음을 체크해주세요.")
            String designatedCock,

            @NotNull(message = "가입비 정보는 필수입니다. 없는 경우 없음을 체크해주세요.")
            Integer joinPrice,

            @NotNull(message = "월 회비 정보는 필수입니다. 없는 경우 없음을 체크해주세요.")
            Integer price,

            @NotNull(message = "최저 나이 정보는 필수입니다.")
            Integer minBirthYear,

            @NotNull(message = "최대 나이 정보는 필수입니다.")
            Integer maxBirthYear,

            @Size(max = 45, message = "모임 소개는 최대 45글자입니다.")
            String content,

            String imgUrl
    ){
        public List<Level> toFemaleLevelEnumList() {
            if (femaleLevel == null) return null;
            try {
                return femaleLevel.stream()
                        .map(Level::fromKorean)
                        .toList();
            } catch (GeneralException e) {
                throw new PartyException(PartyErrorCode.INVALID_LEVEL_FORMAT);
            }
        }

        public List<Level> toMaleLevelEnumList() {
            if (maleLevel == null) return null;
            try {
                return maleLevel.stream()
                        .map(Level::fromKorean)
                        .toList();
            } catch (GeneralException e) {
                throw new PartyException(PartyErrorCode.INVALID_LEVEL_FORMAT);
            }
        }

        public ParticipationType toParticipationTypeEnum() {
            try {
                return ParticipationType.valueOf(partyType);
            } catch (IllegalArgumentException e) {
                throw new PartyException(PartyErrorCode.INVALID_PARTY_TYPE);
            }
        }

        public ActivityTime toActivityTimeEnum() {
            try {
                return ActivityTime.valueOf(activityTime);
            } catch (IllegalArgumentException e) {
                throw new PartyException(PartyErrorCode.INVALID_ACTIVITY_TIME);
            }
        }

        public List<ActiveDay> toActiveDayEnumList() {
            try {
                return activityDay.stream()
                        .map(ActiveDay::valueOf)
                        .toList();
            } catch (IllegalArgumentException e) {
                throw new PartyException(PartyErrorCode.INVALID_ACTIVITY_DAY);
            }
        }
    }

    @Builder
    public record Command(
            String partyName,
            ParticipationType partyType,
            List<Level> femaleLevel,
            List<Level> maleLevel,
            List<ActiveDay> activityDay,
            ActivityTime activityTime,
            Integer minBirthYear,
            Integer maxBirthYear,
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