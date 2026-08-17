package umc.cockple.demo.domain.exercise.service.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.exception.ExerciseException;
import umc.cockple.demo.domain.exercise.service.ExerciseValidator;
import umc.cockple.demo.domain.exercise.service.command.model.ExerciseGameHostChangeCommand;
import umc.cockple.demo.domain.exercise.service.command.result.ExerciseGameHostChangeResult;
import umc.cockple.demo.domain.exercise.service.support.reader.ExerciseReader;
import umc.cockple.demo.domain.member.service.query.lookup.MemberPartyLookupService;

@Service
@Transactional
@RequiredArgsConstructor
public class ExerciseGameHostCommandService {

    private final ExerciseReader exerciseReader;
    private final ExerciseValidator exerciseValidator;
    private final MemberPartyLookupService memberPartyLookupService;

    public ExerciseGameHostChangeResult changeGameHost(
            Long exerciseId,
            Long memberId,
            ExerciseGameHostChangeCommand command) {
        Exercise exercise = exerciseReader.findByIdOrThrow(exerciseId);
        exerciseValidator.validateGameHostManagementPermission(exercise, memberId);

        //  26-08-17 회원 탈퇴 시 동시성 문제로 인해 비관적 락 도입
        memberPartyLookupService.findActiveMemberForUpdate(
                        exercise.getParty().getId(), command.participantId())
                .orElseThrow(() -> new ExerciseException(
                        ExerciseErrorCode.INVALID_GAME_HOST_CANDIDATE));

        exercise.changeGameHost(command.participantId());

        return new ExerciseGameHostChangeResult(exercise.getId(), exercise.getGameHostId());
    }
}
