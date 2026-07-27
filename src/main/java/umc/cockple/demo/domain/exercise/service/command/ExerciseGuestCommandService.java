package umc.cockple.demo.domain.exercise.service.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.exercise.converter.command.ExerciseGuestCommandMapper;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.domain.Guest;
import umc.cockple.demo.domain.exercise.dto.guest.ExerciseGuestInviteDTO;
import umc.cockple.demo.domain.exercise.dto.participation.ExerciseCancelDTO;
import umc.cockple.demo.domain.exercise.repository.GuestRepository;
import umc.cockple.demo.domain.exercise.service.ExerciseValidator;
import umc.cockple.demo.domain.exercise.service.command.model.ExerciseGuestInviteCommand;
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

    private final ExerciseGuestCommandMapper exerciseGuestCommandMapper;

    public ExerciseGuestInviteDTO.Response inviteGuest(
            Long exerciseId, Long inviterId, ExerciseGuestInviteDTO.Request request) {
        log.info("게스트 초대 시작 - exerciseId: {}, inviterId: {}, guestName: {}",
                exerciseId, inviterId, request.guestName());

        Exercise exercise = exerciseReader.findByIdOrThrow(exerciseId);
        Member inviter = memberLookupService.findByIdOrThrow(inviterId);

        exerciseValidator.validateGuestInvitation(exercise, inviter);

        ExerciseGuestInviteCommand command = exerciseGuestCommandMapper
                .toGuestInviteCommand(request, inviter.getId());

        Guest guest = Guest.create(command);
        exercise.addGuest(guest);

        Guest savedGuest = guestRepository.save(guest);

        log.info("게스트 초대 완료 - guestId: {}", savedGuest.getId());
        return exerciseGuestCommandMapper.toGuestInviteResponse(savedGuest, exercise);
    }

    public ExerciseCancelDTO.Response cancelGuestInvitation(Long exerciseId, Long guestId, Long memberId) {
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
        return exerciseGuestCommandMapper.toCancelResponse(exercise, guest);
    }
}
