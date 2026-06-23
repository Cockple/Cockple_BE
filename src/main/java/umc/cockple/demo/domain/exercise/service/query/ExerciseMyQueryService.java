package umc.cockple.demo.domain.exercise.service.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.exercise.converter.ExerciseConverter;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.dto.MyExerciseCalendarDTO;
import umc.cockple.demo.domain.exercise.dto.MyExerciseListDTO;
import umc.cockple.demo.domain.exercise.dto.MyPartyExerciseCalendarDTO;
import umc.cockple.demo.domain.exercise.dto.MyPartyExerciseDTO;
import umc.cockple.demo.domain.exercise.enums.MyExerciseFilterType;
import umc.cockple.demo.domain.exercise.enums.MyExerciseOrderType;
import umc.cockple.demo.domain.exercise.enums.MyPartyExerciseOrderType;
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.exception.ExerciseException;
import umc.cockple.demo.domain.exercise.service.query.lookup.ExerciseParticipantCountLookupService;
import umc.cockple.demo.domain.bookmark.service.query.lookup.ExerciseBookmarkLookupService;
import umc.cockple.demo.domain.exercise.service.support.reader.ExerciseParticipantReader;
import umc.cockple.demo.domain.exercise.service.support.reader.ExerciseReader;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ExerciseMyQueryService {

    private final ExerciseReader exerciseReader;
    private final ExerciseParticipantReader exerciseParticipantReader;
    private final ExerciseParticipantCountLookupService exerciseParticipantCountLookupService;
    private final ExerciseBookmarkLookupService exerciseBookmarkLookupService;
    private final ExerciseConverter exerciseConverter;

    public MyExerciseCalendarDTO.Response getMyExerciseCalendar(Long memberId, LocalDate startDate, LocalDate endDate) {

        log.info("내 운동 캘린더 조회 시작 - memberId = {}, startDate = {}, endDate = {}",
                memberId, startDate, endDate);

        validateDateRange(startDate, endDate);

        DateRange dateRange = DateRange.calculateDateRange(startDate, endDate);

        List<Exercise> exercises = exerciseReader.findByMemberIdAndDateRange(memberId, dateRange.start(), dateRange.end());

        if (exercises.isEmpty()) {
            log.info("해당 기간에 참여한 운동이 없어 빈 응답 반환 - memberId: {}, 기간: {} ~ {}",
                    memberId, dateRange.start(), dateRange.end());
            return exerciseConverter.toEmptyMyCalendarResponse(dateRange.start(), dateRange.end());
        }

        log.info("내 운동 캘린더 조회 완료 - memberId: {}, 조회된 운동 수: {}", memberId, exercises.size());

        return exerciseConverter.toMyCalendarResponse(exercises, dateRange.start(), dateRange.end());
    }

    public MyPartyExerciseDTO.Response getMyPartyExercise(Long memberId) {

        log.info("내 모임 운동 조회 시작 - memberId = {}", memberId);


        List<Long> myPartyIds = exerciseParticipantReader.findPartyIdsByMemberId(memberId);

        if (myPartyIds.isEmpty()) {
            log.info("내가 속한 모임이 없음 - memberId = {}", memberId);
            return exerciseConverter.toEmptyMyPartyExerciseResponse();
        }

        Pageable pageable = PageRequest.of(0, 6);
        List<Exercise> recentExercises = exerciseReader.findRecentByPartyIds(myPartyIds, pageable);

        log.info("내 모임 운동 조회 종료 - 조회된 운동 수 = {}", recentExercises.size());

        return exerciseConverter.toMyPartyExerciseDTO(recentExercises);
    }

    public MyPartyExerciseCalendarDTO.Response getMyPartyExerciseCalendar(
            Long memberId, MyPartyExerciseOrderType orderType, LocalDate startDate, LocalDate endDate) {

        log.info("내 모임 운동 캘린더 조회 시작 - memberId = {}, orderType = {}, 기간 = {}~{}", memberId, orderType, startDate, endDate);

        List<Long> myPartyIds = exerciseParticipantReader.findPartyIdsByMemberId(memberId);

        DateRange dateRange = DateRange.calculateDateRange(startDate, endDate);

        if (myPartyIds.isEmpty()) {
            log.info("내가 속한 모임이 없음 - memberId = {}", memberId);
            return exerciseConverter.toEmptyMyPartyCalendarResponse(dateRange.start(), dateRange.end());
        }

        List<Exercise> exercises = exerciseReader.findByPartyIdsAndDateRange(myPartyIds, dateRange.start(), dateRange.end());

        if (exercises.isEmpty()) {
            log.info("해당 기간에 내 모임의 운동이 없어 빈 응답 반환 - memberId: {}, 기간: {} ~ {}",
                    memberId, dateRange.start(), dateRange.end());
            return exerciseConverter.toEmptyMyPartyCalendarResponse(dateRange.start(), dateRange.end());
        }

        List<Long> exerciseIds = getExerciseIds(exercises);
        Map<Long, Boolean> bookmarkStatus = exerciseBookmarkLookupService.getBookmarkStatus(memberId, exerciseIds);

        Map<Long, Integer> participantCounts = exerciseParticipantCountLookupService.getParticipantCountsByExerciseIdsAndDateRange(
                exerciseIds, dateRange.start(), dateRange.end());

        log.info("내 운동 캘린더 조회 완료 - memberId: {}, 조회된 운동 수: {}", memberId, exercises.size());

        return exerciseConverter.toMyPartyCalendarResponse(
                exercises, dateRange.start(), dateRange.end(), bookmarkStatus, orderType, participantCounts);
    }

    public MyExerciseListDTO.Response getMyExercises(
            Long memberId, MyExerciseFilterType filterType, MyExerciseOrderType orderType, Pageable pageable) {

        log.info("내 참여 운동 조회 시작 - memberId: {}, filterType: {}, orderType: {}",
                memberId, filterType, orderType);


        Pageable sortedPageable = createSortedPageable(pageable, filterType, orderType);

        Slice<Exercise> exerciseSlice = exerciseReader.findByFilterType(memberId, filterType, sortedPageable);

        if (exerciseSlice.isEmpty()) {
            log.info("조회된 운동이 없음 - memberId: {}, filterType: {}", memberId, filterType);
            return exerciseConverter.toEmptyMyExerciseList();
        }

        List<Exercise> exercises = exerciseSlice.getContent();
        List<Long> exerciseIds = exercises.stream().map(Exercise::getId).toList();

        Map<Long, Integer> participantCountMap = exerciseParticipantCountLookupService.getParticipantCountsByExerciseIds(exerciseIds);
        Map<Long, Boolean> bookmarkStatus = exerciseBookmarkLookupService.getBookmarkStatus(memberId, exerciseIds);
        Map<Long, Boolean> isCompletedMap = getExerciseCompletionStatus(exercises);

        log.info("내 참여 운동 조회 완료 - memberId: {}, 조회된 운동 수: {}", memberId, exercises.size());

        return exerciseConverter.toMyExerciseListResponse(exerciseSlice, participantCountMap, bookmarkStatus, isCompletedMap);
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

    private Pageable createSortedPageable(
            Pageable pageable, MyExerciseFilterType filterType, MyExerciseOrderType orderType) {
        Sort sort = createSortByFilterAndOrder(filterType, orderType);
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    private Sort createSortByFilterAndOrder(MyExerciseFilterType filterType, MyExerciseOrderType orderType) {
        return switch (filterType) {
            case ALL -> createSortForAll(orderType);
            case UPCOMING -> createSortForUpcoming(orderType);
            case COMPLETED -> createSortForCompleted(orderType);
        };
    }

    private Map<Long, Boolean> getExerciseCompletionStatus(List<Exercise> exercises) {
        return exercises.stream()
                .collect(Collectors.toMap(
                        Exercise::getId,
                        Exercise::isAlreadyStarted
                ));
    }

    private static List<Long> getExerciseIds(List<Exercise> exercises) {
        return exercises.stream().map(Exercise::getId).toList();
    }

    private Sort createSortForAll(MyExerciseOrderType orderType) {
        return switch (orderType) {
            case LATEST -> Sort.by(
                    Sort.Order.desc("date"),
                    Sort.Order.desc("startTime")
            );
            case OLDEST -> Sort.by(
                    Sort.Order.asc("date"),
                    Sort.Order.asc("startTime")
            );
        };
    }

    private Sort createSortForUpcoming(MyExerciseOrderType orderType) {
        return switch (orderType) {
            case LATEST -> Sort.by(
                    Sort.Order.asc("date"),
                    Sort.Order.asc("startTime")
            );
            case OLDEST -> Sort.by(
                    Sort.Order.desc("date"),
                    Sort.Order.desc("startTime")
            );
        };
    }

    private Sort createSortForCompleted(MyExerciseOrderType orderType) {
        return switch (orderType) {
            case LATEST -> Sort.by(
                    Sort.Order.desc("date"),
                    Sort.Order.desc("startTime")
            );
            case OLDEST -> Sort.by(
                    Sort.Order.asc("date"),
                    Sort.Order.asc("startTime")
            );
        };
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
