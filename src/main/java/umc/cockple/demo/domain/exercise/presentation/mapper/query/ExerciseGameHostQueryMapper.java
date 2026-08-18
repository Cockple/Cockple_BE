package umc.cockple.demo.domain.exercise.presentation.mapper.query;

import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.exercise.presentation.dto.gamehost.ExerciseGameHostDTO;
import umc.cockple.demo.domain.exercise.service.query.result.ExerciseGameHostResult;
import umc.cockple.demo.global.enums.Role;

@Component
public class ExerciseGameHostQueryMapper {

    public ExerciseGameHostDTO.Response toResponse(ExerciseGameHostResult result) {
        return ExerciseGameHostDTO.Response.builder()
                .totalCount(result.totalCount())
                .participants(result.participants().stream()
                        .map(this::toParticipant)
                        .toList())
                .build();
    }

    private ExerciseGameHostDTO.Participant toParticipant(
            ExerciseGameHostResult.Participant participant) {
        return ExerciseGameHostDTO.Participant.builder()
                .participantId(participant.participantId())
                .profileImageUrl(participant.profileImageUrl())
                .partyPosition(toPartyPosition(participant.partyPosition()))
                .isGameHost(participant.gameHost())
                .name(participant.name())
                .gender(participant.gender().name())
                .level(participant.level().getKoreanName())
                .lastExerciseDate(participant.lastExerciseDate())
                .build();
    }

    private String toPartyPosition(Role role) {
        return switch (role) {
            case PARTY_MANAGER -> "모임장";
            case PARTY_SUBMANAGER -> "부모임장";
            case PARTY_MEMBER -> "멤버";
        };
    }
}
