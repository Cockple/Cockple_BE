package umc.cockple.demo.domain.exercise.service.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.domain.Guest;
import umc.cockple.demo.domain.exercise.repository.GuestRepository;
import umc.cockple.demo.domain.exercise.service.ExerciseValidator;
import umc.cockple.demo.domain.exercise.service.command.model.ExerciseGuestInviteCommand;
import umc.cockple.demo.domain.exercise.service.command.result.ExerciseCancelResult;
import umc.cockple.demo.domain.exercise.service.command.result.ExerciseGuestInviteResult;
import umc.cockple.demo.domain.exercise.service.support.reader.ExerciseReader;
import umc.cockple.demo.domain.exercise.service.support.reader.GuestReader;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.service.query.lookup.MemberLookupService;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ExerciseGuestCommandService {

    private final GuestRepository guestRepository;
    private final ExerciseReader exerciseReader;
    private final GuestReader guestReader;
    private final MemberLookupService memberLookupService;

    private final ExerciseValidator exerciseValidator;

    public ExerciseGuestInviteResult inviteGuest(Long exerciseId, ExerciseGuestInviteCommand command) {
        log.info("게스트 초대 시작 - exerciseId: {}, inviterId: {}, guestName: {}",
                exerciseId, command.inviterId(), command.guestName());

        Exercise exercise = exerciseReader.findByIdOrThrow(exerciseId);
        Member inviter = memberLookupService.findByIdOrThrow(command.inviterId());

        exerciseValidator.validateGuestInvitation(exercise, inviter);

        Guest guest = Guest.create(command);
        exercise.addGuest(guest);

        Guest savedGuest = guestRepository.save(guest);

        log.info("게스트 초대 완료 - guestId: {}", savedGuest.getId());
        return new ExerciseGuestInviteResult(
                savedGuest.getId(), savedGuest.getCreatedAt(), exercise.getNowCapacity());
    }

    public ExerciseCancelResult cancelGuestInvitation(Long exerciseId, Long guestId, Long memberId) {
        log.info("게스트 초대 취소 시작 - exerciseId: {}, guestId: {}, memberId: {}",
                exerciseId, guestId, memberId);

        Exercise exercise = exerciseReader.findByIdOrThrow(exerciseId);
        Member member = memberLookupService.findByIdOrThrow(memberId);
        Guest guest = guestReader.findByIdOrThrow(guestId);

        exerciseValidator.validateCancelGuestInvitation(exercise, guest, member);

        exercise.removeGuest(guest);
        guestRepository.delete(guest);

        log.info("게스트 초대 취소 완료 - exerciseId: {}, guestId: {}, memberId: {}",
                exercise.getId(), guest.getId(), member.getId());
        return new ExerciseCancelResult(guest.getGuestName(), exercise.getNowCapacity());
    }
}
