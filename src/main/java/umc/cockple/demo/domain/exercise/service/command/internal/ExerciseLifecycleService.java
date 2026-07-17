package umc.cockple.demo.domain.exercise.service.command.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.exercise.converter.command.ExerciseLifecycleCommandMapper;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.dto.lifecycle.ExerciseCreateDTO;
import umc.cockple.demo.domain.exercise.dto.lifecycle.ExerciseDeleteDTO;
import umc.cockple.demo.domain.exercise.dto.lifecycle.ExerciseUpdateDTO;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.exercise.service.ExerciseValidator;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.repository.PartyRepository;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ExerciseLifecycleService {

    private final ExerciseRepository exerciseRepository;
    private final PartyRepository partyRepository;

    private final ExerciseValidator exerciseValidator;

    private final ExerciseLifecycleCommandMapper exerciseLifecycleCommandMapper;

    public ExerciseCreateDTO.Response createExercise(Party party, Member member, ExerciseCreateDTO.Request request) {
        exerciseValidator.validateCreateExercise(member.getId(), request, party);

        ExerciseCreateDTO.Command exerciseCommand = exerciseLifecycleCommandMapper.toCreateCommand(request);
        ExerciseCreateDTO.AddrCommand addrCommand = exerciseLifecycleCommandMapper.toAddrCreateCommand(request);

        Exercise exercise = party.createExercise(exerciseCommand, addrCommand);
        party.addExercise(exercise);

        Exercise savedExercise = exerciseRepository.save(exercise);

        log.info("운동 생성 완료 - 운동ID: {}", savedExercise.getId());

        return exerciseLifecycleCommandMapper.toCreateResponse(savedExercise);
    }

    public ExerciseDeleteDTO.Response deleteExercise(Exercise exercise, Member member) {
        exerciseValidator.validateDeleteExercise(exercise, member.getId());

        Party party = exercise.getParty();
        party.removeExercise(exercise);
        exerciseRepository.delete(exercise);

        partyRepository.save(party);

        log.info("운동 삭제 종료 - exerciseId: {}, memberId: {}", exercise.getId(), member.getId());

        return exerciseLifecycleCommandMapper.toDeleteResponse(exercise);
    }

    public ExerciseUpdateDTO.Response updateExercise(Exercise exercise, Member member, ExerciseUpdateDTO.Request request) {
        exerciseValidator.validateUpdateExercise(exercise, member, request);

        ExerciseUpdateDTO.Command updateCommand = exerciseLifecycleCommandMapper.toUpdateCommand(request);
        ExerciseUpdateDTO.AddrCommand addrUpdateCommand = exerciseLifecycleCommandMapper.toAddrUpdateCommand(request);

        exercise.updateExerciseInfo(updateCommand);
        exercise.updateExerciseAddr(addrUpdateCommand);

        Exercise savedExercise = exerciseRepository.save(exercise);

        log.info("운동 수정 완료 - exerciseId: {}", savedExercise.getId());

        return exerciseLifecycleCommandMapper.toUpdateResponse(savedExercise);
    }
}
