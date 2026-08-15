package umc.cockple.demo.domain.exercise.presentation.mapper.query;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import umc.cockple.demo.domain.exercise.enums.ExerciseMemberShipStatus;
import umc.cockple.demo.domain.exercise.presentation.dto.lifecycle.ExerciseDetailDTO;
import umc.cockple.demo.domain.exercise.service.query.result.ExerciseBuildingDetailResult;
import umc.cockple.demo.domain.exercise.service.query.result.ExerciseDetailResult;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.global.enums.Role;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Exercise query mapper transport formatting")
class ExerciseTransportFormattingQueryMapperTest {

    @Test
    @DisplayName("참가자 의미 타입은 presentation 경계에서 문자열로 변환한다")
    void formatParticipantEnums() {
        ExerciseDetailResult.ParticipantInfo result = new ExerciseDetailResult.ParticipantInfo(
                1L,
                1,
                null,
                "참가자",
                Gender.MALE,
                Level.B,
                ExerciseMemberShipStatus.PARTY_MEMBER,
                Role.PARTY_MANAGER,
                null,
                LocalDateTime.of(2026, 8, 15, 10, 0),
                false
        );

        ExerciseDetailDTO.ParticipantInfo response =
                new ExerciseParticipantInfoQueryMapper().toParticipantInfo(result);

        assertThat(response.gender()).isEqualTo("MALE");
        assertThat(response.level()).isEqualTo("B");
        assertThat(response.participantType()).isEqualTo("PARTY_MEMBER");
        assertThat(response.partyPosition()).isEqualTo("PARTY_MANAGER");
    }

    @Test
    @DisplayName("요일은 presentation 경계에서 날짜로부터 계산한다")
    void formatDayOfWeek() {
        ExerciseBuildingDetailResult result = ExerciseBuildingDetailResult.builder()
                .date(LocalDate.of(2026, 8, 15))
                .exercises(List.of())
                .build();

        String dayOfWeek = new ExerciseMapQueryMapper()
                .toBuildingDetailResponse(result)
                .dayOfWeek();

        assertThat(dayOfWeek).isEqualTo("SATURDAY");
    }
}
