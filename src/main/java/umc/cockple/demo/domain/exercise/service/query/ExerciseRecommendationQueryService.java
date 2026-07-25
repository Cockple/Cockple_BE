package umc.cockple.demo.domain.exercise.service.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.exercise.converter.query.ExerciseRecommendationQueryMapper;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.dto.recommendation.ExerciseRecommendationCalendarDTO;
import umc.cockple.demo.domain.exercise.dto.recommendation.ExerciseRecommendationDTO;
import umc.cockple.demo.domain.exercise.enums.MyPartyExerciseOrderType;
import umc.cockple.demo.domain.exercise.repository.support.ExerciseRecommendationSearchCondition;
import umc.cockple.demo.domain.exercise.service.query.lookup.ExerciseParticipantCountLookupService;
import umc.cockple.demo.domain.bookmark.service.query.lookup.ExerciseBookmarkLookupService;
import umc.cockple.demo.domain.exercise.service.query.model.ExerciseRecommendationFilterCondition;
import umc.cockple.demo.domain.exercise.service.support.ExerciseDistanceCalculator;
import umc.cockple.demo.domain.exercise.service.support.reader.ExerciseReader;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberAddr;
import umc.cockple.demo.domain.member.service.query.lookup.MemberLookupService;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ExerciseRecommendationQueryService {

    private final ExerciseReader exerciseReader;
    private final ExerciseBookmarkLookupService exerciseBookmarkLookupService;
    private final ExerciseParticipantCountLookupService exerciseParticipantCountLookupService;
    private final ExerciseDistanceCalculator exerciseDistanceCalculator;
    private final MemberLookupService memberLookupService;
    private final ExerciseRecommendationQueryMapper exerciseRecommendationMapper;

    public ExerciseRecommendationDTO.Response getRecommendedExercises(Long memberId) {

        log.info("운동 추천 조회 시작 - memberId: {}", memberId);

        Member member = memberLookupService.findWithAddressesOrThrow(memberId);
        MemberAddr mainAddr = memberLookupService.findMainAddressOrThrow(member);

        List<Exercise> candidateExercises = exerciseReader.findRecommendedExercises(
                memberId, member.getGender(), member.getLevel(), member.getBirth().getYear());

        List<ExerciseWithDistance> finalExercisesWithDistance = getFinalSortedExercises(candidateExercises, mainAddr);
        List<Exercise> finalExercises = extractExercises(finalExercisesWithDistance);

        List<Long> exerciseIds = getExerciseIds(finalExercises);
        Map<Long, Boolean> bookmarkStatus = exerciseBookmarkLookupService.getBookmarkStatus(memberId, exerciseIds);

        log.info("운동 추천 조회 종료 - memberId: {}, 결과 : {}", memberId, exerciseIds.size());

        return exerciseRecommendationMapper.toExerciseRecommendationResponse(finalExercises, bookmarkStatus);
    }

    public ExerciseRecommendationCalendarDTO.Response getRecommendedExerciseCalendar(
            Long memberId,
            LocalDate startDate,
            LocalDate endDate,
            Boolean isCockpleRecommend,
            ExerciseRecommendationFilterCondition filterCondition,
            MyPartyExerciseOrderType sortType) {

        log.info("사용자 추천 운동 캘린더 조회 시작 - memberId: {}, 콕플추천: {}, 필터: {}, 정렬: {}, 기간: {}~{}"
                , memberId, isCockpleRecommend, filterCondition, sortType, startDate, endDate);

        Member member = memberLookupService.findWithAddressesOrThrow(memberId);
        DateRange dateRange = DateRange.calculateDateRange(startDate, endDate);

        List<Exercise> exercises;

        if (isCockpleRecommend) {
            exercises = exerciseReader.findCockpleRecommendedByDateRange(member, dateRange.start(), dateRange.end());
        } else {
            ExerciseRecommendationSearchCondition searchCondition = toSearchCondition(filterCondition);
            exercises = exerciseReader.findFilteredRecommended(member, dateRange.start(), dateRange.end(), searchCondition);
        }

        List<Long> exerciseIds = getExerciseIds(exercises);
        Map<Long, Boolean> bookmarkStatus = exerciseBookmarkLookupService.getBookmarkStatus(memberId, exerciseIds);
        Map<Long, Integer> participantCountMap = exerciseParticipantCountLookupService.getParticipantCountsByExerciseIds(exerciseIds);
        MemberAddr mainAddr = memberLookupService.findMainAddressOrThrow(member);

        log.info("사용자 추천 운동 캘린더 조회 완료 - memberId: {}, 결과 수: {}", memberId, exercises.size());

        return exerciseRecommendationMapper.toRecommendationCalendarResponse(
                exercises, bookmarkStatus, participantCountMap, mainAddr
                , dateRange.start(), dateRange.end(), isCockpleRecommend, sortType);
    }

    private static ExerciseRecommendationSearchCondition toSearchCondition(
            ExerciseRecommendationFilterCondition filterCondition) {
        return new ExerciseRecommendationSearchCondition(
                filterCondition.addr1(),
                filterCondition.addr2(),
                filterCondition.levels(),
                filterCondition.participationTypes(),
                filterCondition.activityTimes()
        );
    }

    private List<ExerciseWithDistance> getFinalSortedExercises(List<Exercise> candidateExercises, MemberAddr mainAddr) {
        return candidateExercises.stream()
                .map(exercise -> {
                    double distance = exerciseDistanceCalculator.calculate(
                            mainAddr.getLatitude(),
                            mainAddr.getLongitude(),
                            exercise.getExerciseAddr().getLatitude(),
                            exercise.getExerciseAddr().getLongitude()
                    );
                    return new ExerciseWithDistance(exercise, distance);
                })
                .sorted(Comparator
                        .comparing(ExerciseWithDistance::distance)
                        .thenComparing(ewd -> ewd.exercise().getDate())
                        .thenComparing(ewd -> ewd.exercise().getStartTime())
                )
                .limit(10)
                .toList();
    }

    private static List<Exercise> extractExercises(List<ExerciseWithDistance> finalExercisesWithDistance) {
        return finalExercisesWithDistance.stream()
                .map(ExerciseWithDistance::exercise)
                .toList();
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

    private record ExerciseWithDistance(Exercise exercise, double distance) {
    }
}
