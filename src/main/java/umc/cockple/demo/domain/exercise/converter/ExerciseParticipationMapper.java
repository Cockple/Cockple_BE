package umc.cockple.demo.domain.exercise.converter;

import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.domain.Guest;
import umc.cockple.demo.domain.exercise.dto.ExerciseCancelDTO;
import umc.cockple.demo.domain.exercise.dto.ExerciseJoinDTO;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberExercise;

@Component
public class ExerciseParticipationMapper {

    public ExerciseJoinDTO.Response toJoinResponse(MemberExercise memberExercise, Exercise exercise) {
        return ExerciseJoinDTO.Response.builder()
                .participantId(memberExercise.getId())
                .joinedAt(memberExercise.getCreatedAt())
                .currentParticipants(exercise.getNowCapacity())
                .build();
    }

    public ExerciseCancelDTO.Response toCancelResponse(Exercise exercise, Member member) {
        return ExerciseCancelDTO.Response.builder()
                .memberName(member.getMemberName())
                .currentParticipants(exercise.getNowCapacity())
                .build();
    }

    public ExerciseCancelDTO.Response toCancelResponse(Exercise exercise, Guest guest) {
        return ExerciseCancelDTO.Response.builder()
                .memberName(guest.getGuestName())
                .currentParticipants(exercise.getNowCapacity())
                .build();
    }
}
