package umc.cockple.demo.domain.exercise.service.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.exercise.converter.query.PartyExerciseCalendarQueryMapper;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.dto.party.PartyExerciseCalendarDTO;
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.exception.ExerciseException;
import umc.cockple.demo.domain.exercise.service.query.lookup.ExerciseParticipantCountLookupService;
import umc.cockple.demo.domain.bookmark.service.query.lookup.ExerciseBookmarkLookupService;
import umc.cockple.demo.domain.exercise.service.support.reader.MemberExerciseReader;
import umc.cockple.demo.domain.exercise.service.support.reader.ExerciseReader;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.service.query.lookup.MemberLookupService;
import umc.cockple.demo.domain.member.service.query.lookup.MemberPartyLookupService;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.enums.PartyStatus;
import umc.cockple.demo.domain.party.exception.PartyErrorCode;
import umc.cockple.demo.domain.party.exception.PartyException;
import umc.cockple.demo.domain.party.service.query.lookup.PartyLookupService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class PartyExerciseQueryService {

    private final ExerciseReader exerciseReader;
    private final MemberExerciseReader memberExerciseReader;
    private final ExerciseParticipantCountLookupService exerciseParticipantCountLookupService;
    private final ExerciseBookmarkLookupService exerciseBookmarkLookupService;
    private final MemberLookupService memberLookupService;
    private final PartyLookupService partyLookupService;
    private final MemberPartyLookupService memberPartyLookupService;
    private final PartyExerciseCalendarQueryMapper partyExerciseCalendarMapper;

    public PartyExerciseCalendarDTO.Response getPartyExerciseCalendar(
            Long partyId, Long memberId, LocalDate startDate, LocalDate endDate) {

        log.info("모임 운동 캘린더 조회 시작 - partyId = {}, memberId = {}, startDate = {}, endDate = {}",
                partyId, memberId, startDate, endDate);

        Party party = partyLookupService.findByIdWithLevelsOrThrow(partyId);
        Member member = memberLookupService.findByIdOrThrow(memberId);
        validateGetPartyExerciseCalendar(startDate, endDate, party);

        Boolean isMember = memberPartyLookupService.isPartyMember(party, member);
        DateRange dateRange = DateRange.calculateDateRange(startDate, endDate);

        List<Exercise> exercises = exerciseReader.findByPartyIdAndDateRange(partyId, dateRange.start(), dateRange.end());

        if (exercises.isEmpty()) {
            log.info("해당 기간에 운동이 없어 빈 응답 반환 - partyId: {}, 기간: {} ~ {}",
                    partyId, dateRange.start(), dateRange.end());

            return partyExerciseCalendarMapper.toEmptyPartyCalendarResponse(
                    dateRange.start(), dateRange.end(), isMember, party);
        }

        Map<Long, Integer> participantCounts = exerciseParticipantCountLookupService.getParticipantCountsByPartyIdAndDateRange(
                partyId, dateRange.start(), dateRange.end());

        List<Long> exerciseIds = getExerciseIds(exercises);
        Map<Long, Boolean> bookmarkStatus = exerciseBookmarkLookupService.getBookmarkStatus(memberId, exerciseIds);
        Map<Long, Boolean> participatingStatus = memberExerciseReader.getParticipatingStatus(memberId, exerciseIds);

        log.info("모임 운동 캘린더 조회 완료 - partyId: {}, 조회된 운동 수: {}", partyId, exercises.size());

        return partyExerciseCalendarMapper.toPartyCalendarResponse(
                exercises, dateRange.start(), dateRange.end(), isMember, party, participantCounts, bookmarkStatus, participatingStatus);
    }

    private void validateGetPartyExerciseCalendar(LocalDate startDate, LocalDate endDate, Party party) {
        validatePartyIsActive(party);
        validateDateRange(startDate, endDate);
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return;
        }

        if (startDate == null || endDate == null) {
            throw new ExerciseException(ExerciseErrorCode.INCOMPLETE_DATE_RANGE);
        }

        if (!startDate.isBefore(endDate)) {
            throw new ExerciseException(ExerciseErrorCode.INVALID_DATE_RANGE);
        }
    }

    private void validatePartyIsActive(Party party) {
        if (party.getStatus() == PartyStatus.INACTIVE) {
            throw new PartyException(PartyErrorCode.PARTY_IS_DELETED);
        }
    }

    private static List<Long> getExerciseIds(List<Exercise> exercises) {
        return exercises.stream().map(Exercise::getId).toList();
    }

    private record DateRange(LocalDate start, LocalDate end) {
        private static DateRange calculateDateRange(LocalDate startDate, LocalDate endDate) {
            if (startDate != null && endDate != null) {
                return new DateRange(startDate, endDate);
            }

            LocalDate today = LocalDate.now();
            LocalDate thisWeekMonday = today.minusDays(today.getDayOfWeek().getValue() - 1);
            LocalDate defaultStart = thisWeekMonday.minusWeeks(1);
            LocalDate defaultEnd = thisWeekMonday.plusWeeks(3).plusDays(6);

            return new DateRange(defaultStart, defaultEnd);
        }
    }
}
