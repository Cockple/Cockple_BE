package umc.cockple.demo.domain.exercise.service.command.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.exercise.converter.ExerciseConverter;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.domain.Guest;
import umc.cockple.demo.domain.exercise.dto.ExerciseCancelDTO;
import umc.cockple.demo.domain.exercise.dto.ExerciseGuestInviteDTO;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.exercise.repository.GuestRepository;
import umc.cockple.demo.domain.exercise.service.ExerciseValidator;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.repository.MemberRepository;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ExerciseGuestService {

    private final ExerciseRepository exerciseRepository;
    private final MemberRepository memberRepository;
    private final GuestRepository guestRepository;

    private final ExerciseValidator exerciseValidator;

    private final ExerciseConverter exerciseConverter;

    public ExerciseGuestInviteDTO.Response inviteGuest(Exercise exercise, Member inviter, ExerciseGuestInviteDTO.Request request) {
        exerciseValidator.validateGuestInvitation(exercise, inviter);

        ExerciseGuestInviteDTO.Command command = exerciseConverter.toGuestInviteCommand(request, inviter.getId());

        Guest guest = Guest.create(command);
        exercise.addGuest(guest);

        Guest savedGuest = guestRepository.save(guest);

        log.info("게스트 초대 완료 - guestId: {}", savedGuest.getId());

        return exerciseConverter.toGuestInviteResponse(savedGuest, exercise);
    }

    public ExerciseCancelDTO.Response cancelGuestInvitation(Exercise exercise, Guest guest, Member member) {
        exerciseValidator.validateCancelGuestInvitation(exercise, guest, member);

        exercise.removeGuest(guest);

        guestRepository.delete(guest);

        exerciseRepository.save(exercise);

        log.info("게스트 초대 취소 완료 - exerciseId: {}, guestId: {}, memberId: {}", exercise.getId(), guest.getId(), member.getId());

        return exerciseConverter.toCancelResponse(exercise, guest);
    }
}
