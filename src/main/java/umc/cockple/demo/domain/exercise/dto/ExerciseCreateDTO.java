package umc.cockple.demo.domain.exercise.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Builder;
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.exception.ExerciseException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

public class ExerciseCreateDTO {

    @Builder
    @Schema(name = "ExerciseCreateRequest", description = "운동 생성 요청")
    public record Request(

            @NotBlank(message = "운동 날짜는 필수입니다.")
            @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "날짜 형식: YYYY-MM-DD")
            String date,

            @NotBlank(message = "건물명은 필수입니다.")
            String buildingName,

            @NotBlank(message = "도로명 주소는 필수입니다.")
            String roadAddress,

            @NotNull(message = "위도는 필수입니다")
            @DecimalMin(value = "33.0") @DecimalMax(value = "43.0")
            Double latitude,

            @NotNull(message = "경도는 필수입니다")
            @DecimalMin(value = "124.0") @DecimalMax(value = "132.0")
            Double longitude,

            @NotBlank(message = "시작 시간은 필수입니다.")
            @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "시간 형식: HH:mm")
            String startTime,

            @NotBlank(message = "종료 시간은 필수입니다.")
            @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "시간 형식: HH:mm")
            String endTime,

            @NotNull(message = "모집 인원은 필수입니다.")
            @Min(value = 2, message = "모집 인원은 최소 2명 이상입니다.")
            @Max(value = 45, message = "모집 인원은 최대 45명 이하입니다.")
            Integer maxCapacity,

            @NotNull(message = "모임 멤버 게스트 초대 허용 여부는 필수입니다.")
            Boolean allowMemberGuestsInvitation,

            @NotNull(message = "외부 게스트 허용 여부는 필수입니다.")
            Boolean allowExternalGuests,

            @Size(max = 45, message = "공지사항은 45자를 초과할 수 없습니다")
            String notice
    ) {
        public LocalDate toParsedDate() {
            try {
                return LocalDate.parse(date);
            } catch (DateTimeParseException e) {
                throw new ExerciseException(ExerciseErrorCode.INVALID_DATE_FORMAT);
            }
        }

        public LocalTime toParsedStartTime() {
            try {
                return LocalTime.parse(startTime);
            } catch (DateTimeParseException e) {
                throw new ExerciseException(ExerciseErrorCode.INVALID_START_TIME_FORMAT);
            }
        }

        public LocalTime toParsedEndTime() {
            try {
                return LocalTime.parse(endTime);
            } catch (DateTimeParseException e) {
                throw new ExerciseException(ExerciseErrorCode.INVALID_END_TIME_FORMAT);
            }
        }
    }

    @Builder
    public record Command(
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            Integer maxCapacity,
            Boolean partyGuestAccept,
            Boolean outsideGuestAccept,
            String notice
    ) {
    }

    @Builder
    public record AddrCommand(
            String roadAddress,
            String buildingName,
            Double latitude,
            Double longitude
    ) {
    }

    @Builder
    public record Response(
            Long exerciseId,
            LocalDateTime createdAt
    ) {
    }
}
