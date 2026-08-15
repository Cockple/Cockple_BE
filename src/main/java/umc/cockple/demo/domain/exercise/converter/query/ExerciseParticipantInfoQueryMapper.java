package umc.cockple.demo.domain.exercise.converter.query;

import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.exercise.dto.lifecycle.ExerciseDetailDTO;
import umc.cockple.demo.domain.exercise.service.query.result.ExerciseDetailResult;

@Component
public class ExerciseParticipantInfoQueryMapper {

    public ExerciseDetailDTO.ParticipantInfo toParticipantInfo(ExerciseDetailResult.ParticipantInfo result) {
        return ExerciseDetailDTO.ParticipantInfo.builder()
                .participantId(result.participantId())
                .participantNumber(result.participantNumber())
                .profileImageUrl(result.profileImageUrl())
                .name(result.name())
                .gender(result.gender())
                .level(result.level())
                .participantType(result.participantType())
                .partyPosition(result.partyPosition())
                .inviterName(result.inviterName())
                .joinedAt(result.joinedAt())
                .isWithdrawn(result.withdrawn())
                .build();
    }
}
