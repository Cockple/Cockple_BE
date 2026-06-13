package umc.cockple.demo.domain.exercise.service.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.exercise.converter.ExerciseConverter;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.domain.Guest;
import umc.cockple.demo.domain.exercise.dto.ExerciseDetailDTO;
import umc.cockple.demo.domain.exercise.dto.ExerciseMyGuestListDTO;
import umc.cockple.demo.domain.exercise.service.support.ExerciseParticipantInfoAssembler;
import umc.cockple.demo.domain.exercise.service.support.ExerciseReader;
import umc.cockple.demo.domain.exercise.service.support.GuestReader;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.service.support.MemberLookupService;
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
    private final ExerciseParticipantInfoAssembler participantInfoAssembler;
    private final MemberLookupService memberLookupService;
    private final ExerciseConverter exerciseConverter;

    public ExerciseMyGuestListDTO.Response getMyInvitedGuests(Long exerciseId, Long memberId) {

        log.info("내가 초대한 게스트 조회 시작 - exerciseId = {}, memberId = {}", exerciseId, memberId);

        Exercise exercise = exerciseReader.findExerciseWithBasicInfoOrThrow(exerciseId);
        Member member = memberLookupService.findByIdOrThrow(memberId);

        List<Guest> myGuests = guestReader.findByExerciseIdAndInviterId(exerciseId, memberId);

        if (myGuests.isEmpty()) {
            log.info("초대한 게스트가 없어 빈 응답 반환 - exerciseId: {}, memberId: {}", exerciseId, memberId);
            return exerciseConverter.toEmptyGuestListResponse();
        }

        List<ExerciseDetailDTO.ParticipantInfo> allParticipants = participantInfoAssembler.getAllSortedParticipants(
                exerciseId, exercise.getParty());
        Map<Long, ExerciseMyGuestListDTO.GuestGroups> guestNumberMap = createGuestNumberMap(
                allParticipants, exercise.getMaxCapacity());

        String inviterName = member.getMemberName();
        List<ExerciseMyGuestListDTO.GuestInfo> guestInfoList = buildGuestInfoList(myGuests, guestNumberMap, inviterName);

        ExerciseMyGuestListDTO.GuestStatistics statistics = calculateGuestStatistics(guestInfoList);

        log.info("내가 초대한 게스트 조회 완료 - exerciseId: {}", exerciseId);

        return exerciseConverter.toMyGuestListResponse(statistics, guestInfoList);
    }

    private Map<Long, ExerciseMyGuestListDTO.GuestGroups> createGuestNumberMap(
            List<ExerciseDetailDTO.ParticipantInfo> allParticipants,
            Integer maxCapacity) {

        Map<Long, ExerciseMyGuestListDTO.GuestGroups> guestNumberMap = new HashMap<>();

        for (int i = 0; i < allParticipants.size(); i++) {
            ExerciseDetailDTO.ParticipantInfo participant = allParticipants.get(i);

            if ("GUEST".equals(participant.participantType())) {
                if (i < maxCapacity) {
                    guestNumberMap.put(participant.participantId(),
                            ExerciseMyGuestListDTO.GuestGroups.participant(i + 1));
                } else {
                    int waitingNumber = i - maxCapacity + 1;
                    guestNumberMap.put(participant.participantId(),
                            ExerciseMyGuestListDTO.GuestGroups.waiting(waitingNumber));
                }
            }
        }

        return guestNumberMap;
    }

    private List<ExerciseMyGuestListDTO.GuestInfo> buildGuestInfoList(
            List<Guest> myGuests,
            Map<Long, ExerciseMyGuestListDTO.GuestGroups> guestNumberMap,
            String inviterName) {

        return myGuests.stream()
                .map(guest -> exerciseConverter.toGuestInfo(guest, guestNumberMap, inviterName))
                .toList();
    }

    private ExerciseMyGuestListDTO.GuestStatistics calculateGuestStatistics(
            List<ExerciseMyGuestListDTO.GuestInfo> guestInfoList) {

        int totalCount = guestInfoList.size();
        int maleCount = (int) guestInfoList.stream()
                .filter(guest -> guest.gender() == Gender.MALE)
                .count();
        int femaleCount = totalCount - maleCount;

        return new ExerciseMyGuestListDTO.GuestStatistics(totalCount, maleCount, femaleCount);
    }
}
