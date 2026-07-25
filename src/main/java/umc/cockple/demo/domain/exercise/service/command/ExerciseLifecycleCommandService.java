package umc.cockple.demo.domain.exercise.service.command;

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
import umc.cockple.demo.domain.exercise.service.command.model.ExerciseCreateAddressCommand;
import umc.cockple.demo.domain.exercise.service.command.model.ExerciseCreateCommand;
import umc.cockple.demo.domain.exercise.service.command.model.ExerciseUpdateAddressCommand;
import umc.cockple.demo.domain.exercise.service.command.model.ExerciseUpdateCommand;
import umc.cockple.demo.domain.exercise.service.support.reader.ExerciseReader;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.service.query.lookup.MemberLookupService;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.service.query.lookup.PartyLookupService;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ExerciseLifecycleCommandService {

    private final ExerciseRepository exerciseRepository;
    private final ExerciseReader exerciseReader;
    private final MemberLookupService memberLookupService;
    private final PartyLookupService partyLookupService;

    private final ExerciseValidator exerciseValidator;

    private final ExerciseLifecycleCommandMapper exerciseLifecycleCommandMapper;

    public ExerciseCreateDTO.Response createExercise(Long partyId, Long memberId, ExerciseCreateDTO.Request request) {
        log.info("운동 생성 시작 - partyId: {}, memberId: {}, date: {}", partyId, memberId, request.date());

        Party party = partyLookupService.findByIdOrThrow(partyId);
        Member member = memberLookupService.findByIdOrThrow(memberId);

        exerciseValidator.validateCreateExercise(member.getId(), request, party);

        ExerciseCreateCommand exerciseCommand = exerciseLifecycleCommandMapper.toCreateCommand(request);
        ExerciseCreateAddressCommand addrCommand = exerciseLifecycleCommandMapper.toAddrCreateCommand(request);

        Exercise exercise = party.createExercise(exerciseCommand, addrCommand);
        party.addExercise(exercise);

        Exercise savedExercise = exerciseRepository.save(exercise);

        log.info("운동 생성 완료 - 운동ID: {}", savedExercise.getId());

        return exerciseLifecycleCommandMapper.toCreateResponse(savedExercise);
    }

    public ExerciseDeleteDTO.Response deleteExercise(Long exerciseId, Long memberId) {
        log.info("운동 삭제 시작 - exerciseId: {}, memberId: {}", exerciseId, memberId);

        Exercise exercise = exerciseReader.findByIdOrThrow(exerciseId);
        Member member = memberLookupService.findByIdOrThrow(memberId);

        exerciseValidator.validateDeleteExercise(exercise, member.getId());

        Party party = exercise.getParty();
        party.removeExercise(exercise);
        exerciseRepository.delete(exercise);

        log.info("운동 삭제 종료 - exerciseId: {}, memberId: {}", exercise.getId(), member.getId());

        return exerciseLifecycleCommandMapper.toDeleteResponse(exercise);
    }

    public ExerciseUpdateDTO.Response updateExercise(Long exerciseId, Long memberId, ExerciseUpdateDTO.Request request) {
        log.info("운동 업데이트 시작 - exerciseId: {}, memberId: {}", exerciseId, memberId);

        Exercise exercise = exerciseReader.findByIdOrThrow(exerciseId);
        Member member = memberLookupService.findByIdOrThrow(memberId);

        exerciseValidator.validateUpdateExercise(exercise, member, request);

        ExerciseUpdateCommand updateCommand = exerciseLifecycleCommandMapper.toUpdateCommand(request);
        ExerciseUpdateAddressCommand addrUpdateCommand = exerciseLifecycleCommandMapper.toAddrUpdateCommand(request);

        exercise.updateExerciseInfo(updateCommand);
        exercise.updateExerciseAddr(addrUpdateCommand);

        exerciseRepository.flush();

        log.info("운동 수정 완료 - exerciseId: {}", exercise.getId());

        return exerciseLifecycleCommandMapper.toUpdateResponse(exercise);
    }
}
