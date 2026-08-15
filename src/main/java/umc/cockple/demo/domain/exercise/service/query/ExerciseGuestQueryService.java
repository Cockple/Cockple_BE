package umc.cockple.demo.domain.exercise.service.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.domain.Guest;
import umc.cockple.demo.domain.exercise.service.query.model.ExerciseParticipantPosition;
import umc.cockple.demo.domain.exercise.service.query.model.ExerciseParticipantSnapshot;
import umc.cockple.demo.domain.exercise.service.query.result.ExerciseMyGuestListResult;
import umc.cockple.demo.domain.exercise.service.support.assembler.ExerciseParticipantSnapshotAssembler;
import umc.cockple.demo.domain.exercise.service.support.calculator.ExerciseParticipantPositionCalculator;
import umc.cockple.demo.domain.exercise.service.support.reader.ExerciseReader;
import umc.cockple.demo.domain.exercise.service.support.reader.GuestReader;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.service.query.lookup.MemberLookupService;
import umc.cockple.demo.global.enums.Gender;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ExerciseGuestQueryService {

    private final ExerciseReader exerciseReader;
    private final GuestReader guestReader;
    private final ExerciseParticipantSnapshotAssembler participantSnapshotAssembler;
    private final ExerciseParticipantPositionCalculator participantPositionCalculator;
    private final MemberLookupService memberLookupService;

    public ExerciseMyGuestListResult getMyInvitedGuests(Long exerciseId, Long memberId) {

        log.info("내가 초대한 게스트 조회 시작 - exerciseId = {}, memberId = {}", exerciseId, memberId);

        Exercise exercise = exerciseReader.findExerciseWithBasicInfoOrThrow(exerciseId);
        Member member = memberLookupService.findByIdOrThrow(memberId);

        List<Guest> myGuests = guestReader.findByExerciseIdAndInviterId(exerciseId, memberId);

        if (myGuests.isEmpty()) {
            log.info("초대한 게스트가 없어 빈 응답 반환 - exerciseId: {}, memberId: {}", exerciseId, memberId);
            return ExerciseMyGuestListResult.empty();
        }

        List<ExerciseParticipantSnapshot> allParticipants = participantSnapshotAssembler.getAllParticipants(
                exerciseId, exercise.getParty());
        List<ExerciseParticipantPosition> participantPositions =
                participantPositionCalculator.calculate(allParticipants, exercise.getMaxCapacity());
        Map<Long, ExerciseParticipantPosition> guestPositionById = indexGuestPositions(participantPositions);

        String inviterName = member.getMemberName();
        List<ExerciseMyGuestListResult.GuestInfo> guestInfoList =
                buildGuestInfoList(myGuests, guestPositionById, inviterName);

        log.info("내가 초대한 게스트 조회 완료 - exerciseId: {}", exerciseId);

        int maleCount = (int) guestInfoList.stream()
                .filter(guest -> guest.gender() == Gender.MALE)
                .count();
        return new ExerciseMyGuestListResult(
                guestInfoList.size(), maleCount, guestInfoList.size() - maleCount, guestInfoList);
    }

    private Map<Long, ExerciseParticipantPosition> indexGuestPositions(
            List<ExerciseParticipantPosition> participantPositions) {

        return participantPositions.stream()
                .filter(position -> position.participant().isGuest())
                .collect(Collectors.toMap(
                        position -> position.participant().participantId(),
                        position -> position));
    }

    private List<ExerciseMyGuestListResult.GuestInfo> buildGuestInfoList(
            List<Guest> myGuests,
            Map<Long, ExerciseParticipantPosition> guestPositionById,
            String inviterName) {

        return myGuests.stream()
                .map(guest -> toGuestInfo(guest, guestPositionById, inviterName))
                .toList();
    }

    private ExerciseMyGuestListResult.GuestInfo toGuestInfo(
            Guest guest,
            Map<Long, ExerciseParticipantPosition> guestPositionById,
            String inviterName) {
        ExerciseParticipantPosition guestPosition = guestPositionById.get(guest.getId());

        return new ExerciseMyGuestListResult.GuestInfo(
                guest.getId(),
                guestPosition.waiting(),
                guestPosition.participantNumber(),
                guest.getGuestName(),
                guest.getGender(),
                guest.getLevel(),
                inviterName
        );
    }
}
