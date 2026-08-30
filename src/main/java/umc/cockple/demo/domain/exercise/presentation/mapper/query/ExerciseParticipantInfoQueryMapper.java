package umc.cockple.demo.domain.exercise.presentation.mapper.query;

import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.exercise.presentation.dto.lifecycle.ExerciseDetailDTO;
import umc.cockple.demo.domain.exercise.service.query.result.ExerciseDetailResult;

@Component
public class ExerciseParticipantInfoQueryMapper {

    public ExerciseDetailDTO.ParticipantInfo toParticipantInfo(ExerciseDetailResult.ParticipantInfo result) {
        return ExerciseDetailDTO.ParticipantInfo.builder()
                .participantId(result.participantId())
                .participantNumber(result.participantNumber())
                .profileImageUrl(result.profileImageUrl())
                .name(result.name())
                .gender(result.gender().name())
                .level(result.level().name())
                .participantType(result.participantType().name())
                .partyPosition(result.partyPosition() != null ? result.partyPosition().name() : null)
                .inviterName(result.inviterName())
                .joinedAt(result.joinedAt())
                .isWithdrawn(result.withdrawn())
                .build();
    }
}
