package umc.cockple.demo.domain.exercise.presentation.mapper.command;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import umc.cockple.demo.domain.exercise.presentation.dto.gamehost.ExerciseGameHostDTO;
import umc.cockple.demo.domain.exercise.service.command.model.ExerciseGameHostChangeCommand;
import umc.cockple.demo.domain.exercise.service.command.result.ExerciseGameHostChangeResult;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ExerciseGameHostCommandMapper")
class ExerciseGameHostCommandMapperTest {

    private final ExerciseGameHostCommandMapper mapper = new ExerciseGameHostCommandMapper();

    @Test
    @DisplayName("변경 요청과 결과를 service 모델과 응답 DTO로 변환한다")
    void mapsChangeRequestAndResponse() {
        ExerciseGameHostChangeCommand command = mapper.toChangeCommand(
                new ExerciseGameHostDTO.ChangeRequest(3L));

        ExerciseGameHostDTO.ChangeResponse response = mapper.toChangeResponse(
                new ExerciseGameHostChangeResult(10L, command.participantId()));

        assertThat(command.participantId()).isEqualTo(3L);
        assertThat(response.exerciseId()).isEqualTo(10L);
        assertThat(response.participantId()).isEqualTo(3L);
    }
}
