package umc.cockple.demo.domain.exercise.service.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.events.ExerciseDeletedEvent;
import umc.cockple.demo.domain.exercise.events.ExerciseUpdatedEvent;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.exercise.service.ExerciseValidator;
import umc.cockple.demo.domain.exercise.service.command.model.ExerciseCreateAddressCommand;
import umc.cockple.demo.domain.exercise.service.command.model.ExerciseCreateCommand;
import umc.cockple.demo.domain.exercise.service.command.model.ExerciseUpdateAddressCommand;
import umc.cockple.demo.domain.exercise.service.command.model.ExerciseUpdateCommand;
import umc.cockple.demo.domain.exercise.service.command.result.ExerciseCreateResult;
import umc.cockple.demo.domain.exercise.service.command.result.ExerciseDeleteResult;
import umc.cockple.demo.domain.exercise.service.command.result.ExerciseUpdateResult;
import umc.cockple.demo.domain.exercise.service.support.reader.ExerciseReader;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.service.query.lookup.MemberLookupService;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.service.query.lookup.PartyLookupService;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ExerciseLifecycleCommandService {

    private final ExerciseRepository exerciseRepository;
    private final ExerciseReader exerciseReader;
    private final MemberLookupService memberLookupService;
    private final PartyLookupService partyLookupService;
    private final ApplicationEventPublisher eventPublisher;

    private final ExerciseValidator exerciseValidator;

    public ExerciseCreateResult createExercise(
            Long partyId,
            Long memberId,
            ExerciseCreateCommand exerciseCommand,
            ExerciseCreateAddressCommand addressCommand) {
        log.info("운동 생성 시작 - partyId: {}, memberId: {}, date: {}", partyId, memberId, exerciseCommand.date());

        Party party = partyLookupService.findByIdOrThrow(partyId);
        Member member = memberLookupService.findByIdOrThrow(memberId);

        exerciseValidator.validateCreateExercise(member.getId(), exerciseCommand, party);

        Exercise exercise = party.createExercise(exerciseCommand, addressCommand);
        party.addExercise(exercise);

        Exercise savedExercise = exerciseRepository.save(exercise);

        log.info("운동 생성 완료 - 운동ID: {}", savedExercise.getId());

        return new ExerciseCreateResult(savedExercise.getId(), savedExercise.getCreatedAt());
    }

    public ExerciseDeleteResult deleteExercise(Long exerciseId, Long memberId) {
        log.info("운동 삭제 시작 - exerciseId: {}, memberId: {}", exerciseId, memberId);

        Exercise exercise = exerciseReader.findByIdOrThrow(exerciseId);
        Member member = memberLookupService.findByIdOrThrow(memberId);

        exerciseValidator.validateDeleteExercise(exercise, member.getId());

        Party party = exercise.getParty();
        List<Long> recipientMemberIds = recipientMemberIds(exercise);
        eventPublisher.publishEvent(ExerciseDeletedEvent.deleted(
                exercise.getId(),
                party.getId(),
                party.getPartyName(),
                party.getPartyImg() != null ? party.getPartyImg().getImgKey() : null,
                exercise.getDate(),
                recipientMemberIds
        ));
        party.removeExercise(exercise);
        exerciseRepository.delete(exercise);

        log.info("운동 삭제 종료 - exerciseId: {}, memberId: {}", exercise.getId(), member.getId());

        return new ExerciseDeleteResult(exercise.getId());
    }

    public ExerciseUpdateResult updateExercise(
            Long exerciseId,
            Long memberId,
            ExerciseUpdateCommand updateCommand,
            ExerciseUpdateAddressCommand addressCommand) {
        log.info("운동 업데이트 시작 - exerciseId: {}, memberId: {}", exerciseId, memberId);

        Exercise exercise = exerciseReader.findByIdOrThrow(exerciseId);
        Member member = memberLookupService.findByIdOrThrow(memberId);

        exerciseValidator.validateUpdateExercise(exercise, member, updateCommand);

        exercise.updateExerciseInfo(updateCommand);
        exercise.updateExerciseAddr(addressCommand);

        exerciseRepository.flush();

        Party party = exercise.getParty();
        eventPublisher.publishEvent(ExerciseUpdatedEvent.updated(
                exercise.getId(),
                party.getId(),
                party.getPartyName(),
                party.getPartyImg() != null ? party.getPartyImg().getImgKey() : null,
                exercise.getDate(),
                recipientMemberIds(exercise)
        ));

        log.info("운동 수정 완료 - exerciseId: {}", exercise.getId());

        return new ExerciseUpdateResult(exercise.getId(), exercise.getUpdatedAt());
    }

    private List<Long> recipientMemberIds(Exercise exercise) {
        return exercise.getMemberExercises().stream()
                .map(memberExercise -> memberExercise.getMember().getId())
                .toList();
    }
}
