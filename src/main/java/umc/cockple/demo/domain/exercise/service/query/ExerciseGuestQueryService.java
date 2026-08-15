package umc.cockple.demo.domain.exercise.service.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.domain.Guest;
import umc.cockple.demo.domain.exercise.service.query.model.ExerciseParticipantSnapshot;
import umc.cockple.demo.domain.exercise.service.query.result.ExerciseMyGuestListResult;
import umc.cockple.demo.domain.exercise.service.support.assembler.ExerciseParticipantSnapshotAssembler;
import umc.cockple.demo.domain.exercise.service.support.reader.ExerciseReader;
import umc.cockple.demo.domain.exercise.service.support.reader.GuestReader;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.service.query.lookup.MemberLookupService;
import umc.cockple.demo.global.enums.Gender;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ExerciseGuestQueryService {

    private final ExerciseReader exerciseReader;
    private final GuestReader guestReader;
    private final ExerciseParticipantSnapshotAssembler participantSnapshotAssembler;
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

        List<ExerciseParticipantSnapshot> allParticipants = participantSnapshotAssembler.getAllSortedParticipants(
                exerciseId, exercise.getParty());
        Map<Long, GuestPosition> guestPositionById = createGuestPositionMap(
                allParticipants, exercise.getMaxCapacity());

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

    private Map<Long, GuestPosition> createGuestPositionMap(
            List<ExerciseParticipantSnapshot> allParticipants,
            Integer maxCapacity) {

        Map<Long, GuestPosition> guestPositionById = new HashMap<>();

        for (int i = 0; i < allParticipants.size(); i++) {
            ExerciseParticipantSnapshot participant = allParticipants.get(i);

            if (participant.isGuest()) {
                if (i < maxCapacity) {
                    guestPositionById.put(participant.participantId(), GuestPosition.participant(i + 1));
                } else {
                    int waitingNumber = i - maxCapacity + 1;
                    guestPositionById.put(participant.participantId(), GuestPosition.waiting(waitingNumber));
                }
            }
        }

        return guestPositionById;
    }

    private List<ExerciseMyGuestListResult.GuestInfo> buildGuestInfoList(
            List<Guest> myGuests,
            Map<Long, GuestPosition> guestPositionById,
            String inviterName) {

        return myGuests.stream()
                .map(guest -> toGuestInfo(guest, guestPositionById, inviterName))
                .toList();
    }

    private ExerciseMyGuestListResult.GuestInfo toGuestInfo(
            Guest guest,
            Map<Long, GuestPosition> guestPositionById,
            String inviterName) {
        GuestPosition guestPosition = guestPositionById.get(guest.getId());

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

    private record GuestPosition(
            int participantNumber,
            boolean waiting
    ) {

        private static GuestPosition participant(int number) {
            return new GuestPosition(number, false);
        }

        private static GuestPosition waiting(int number) {
            return new GuestPosition(number, true);
        }
    }
}
