package umc.cockple.demo.domain.exercise.presentation.mapper.query;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import umc.cockple.demo.domain.exercise.presentation.dto.gamehost.ExerciseGameHostDTO;
import umc.cockple.demo.domain.exercise.service.query.result.ExerciseGameHostResult;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.global.enums.Role;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

@DisplayName("ExerciseGameHostQueryMapper")
class ExerciseGameHostQueryMapperTest {

    private final ExerciseGameHostQueryMapper mapper = new ExerciseGameHostQueryMapper();

    @Test
    @DisplayName("역할과 성별과 급수를 공개 응답 문자열로 변환한다")
    void convertsGameHostResponse() {
        LocalDate lastExerciseDate = LocalDate.of(2026, 1, 12);
        ExerciseGameHostResult result = new ExerciseGameHostResult(3, List.of(
                participant(1L, Role.PARTY_MANAGER, true, Gender.FEMALE, Level.D, lastExerciseDate),
                participant(2L, Role.PARTY_SUBMANAGER, false, Gender.MALE, Level.EXPERT, null),
                participant(3L, Role.PARTY_MEMBER, false, Gender.FEMALE, Level.BEGINNER, null)
        ));

        ExerciseGameHostDTO.Response response = mapper.toResponse(result);

        assertThat(response.totalCount()).isEqualTo(3);
        assertThat(response.participants())
                .extracting(
                        ExerciseGameHostDTO.Participant::participantId,
                        ExerciseGameHostDTO.Participant::partyPosition,
                        ExerciseGameHostDTO.Participant::isGameHost,
                        ExerciseGameHostDTO.Participant::gender,
                        ExerciseGameHostDTO.Participant::level,
                        ExerciseGameHostDTO.Participant::lastExerciseDate)
                .containsExactly(
                        tuple(1L, "모임장", true, "FEMALE", "D조", lastExerciseDate),
                        tuple(2L, "부모임장", false, "MALE", "자강", null),
                        tuple(3L, "멤버", false, "FEMALE", "초심", null));
    }

    private ExerciseGameHostResult.Participant participant(
            Long participantId,
            Role role,
            boolean gameHost,
            Gender gender,
            Level level,
            LocalDate lastExerciseDate) {
        return new ExerciseGameHostResult.Participant(
                participantId,
                null,
                role,
                gameHost,
                "테스트 회원",
                gender,
                level,
                lastExerciseDate
        );
    }
}
