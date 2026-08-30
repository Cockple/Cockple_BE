package umc.cockple.demo.domain.exercise.service.support.reader;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.repository.support.ExerciseRecommendationSearchCondition;
import umc.cockple.demo.domain.exercise.enums.MyExerciseFilterType;
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.exception.ExerciseException;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.exercise.service.query.model.ExerciseMapSearchQuery;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ExerciseReader {

    private final ExerciseRepository exerciseRepository;

    public Exercise findByIdOrThrow(Long exerciseId) {
        return exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new ExerciseException(ExerciseErrorCode.EXERCISE_NOT_FOUND));
    }

    public Exercise findByIdWithPartyLevelsOrThrow(Long exerciseId) {
        return exerciseRepository.findByIdWithPartyLevels(exerciseId)
                .orElseThrow(() -> new ExerciseException(ExerciseErrorCode.EXERCISE_NOT_FOUND));
    }

    public Exercise findExerciseWithBasicInfoOrThrow(Long exerciseId) {
        return exerciseRepository.findExerciseWithBasicInfo(exerciseId)
                .orElseThrow(() -> new ExerciseException(ExerciseErrorCode.EXERCISE_NOT_FOUND));
    }

    public List<Exercise> findByPartyIdAndDateRange(Long partyId, LocalDate startDate, LocalDate endDate) {
        return exerciseRepository.findByPartyIdAndDateRange(partyId, startDate, endDate);
    }

    public List<Exercise> findByMemberIdAndDateRange(Long memberId, LocalDate startDate, LocalDate endDate) {
        return exerciseRepository.findByMemberIdAndDateRange(memberId, startDate, endDate);
    }

    public List<Exercise> findRecentByPartyIds(List<Long> partyIds, Pageable pageable) {
        return exerciseRepository.findRecentExercisesByPartyIds(partyIds, pageable);
    }

    public List<Exercise> findRecommendedExercises(Long memberId, Gender gender, Level level, int birthYear) {
        return exerciseRepository.findExercisesByMemberIdAndLevelAndBirthYear(memberId, gender, level, birthYear);
    }

    public List<Exercise> findByPartyIdsAndDateRange(List<Long> partyIds, LocalDate startDate, LocalDate endDate) {
        return exerciseRepository.findByPartyIdsAndDateRange(partyIds, startDate, endDate);
    }

    public Slice<Exercise> findByFilterType(Long memberId, MyExerciseFilterType filterType, Pageable pageable) {
        return switch (filterType) {
            case ALL -> exerciseRepository.findMyExercisesWithPaging(memberId, pageable);
            case UPCOMING -> exerciseRepository.findMyUpcomingExercisesWithPaging(memberId, pageable);
            case COMPLETED -> exerciseRepository.findMyCompletedExercisesWithPaging(memberId, pageable);
        };
    }

    public List<Exercise> findByBuildingAndDate(String buildingName, String streetAddr, LocalDate date) {
        return exerciseRepository.findExercisesByBuildingAndDate(buildingName, streetAddr, date);
    }

    public List<Exercise> findByMonthAndRadius(
            LocalDate startDate,
            LocalDate endDate,
            ExerciseMapSearchQuery searchQuery) {
        return exerciseRepository.findExercisesByMonthAndRadius(
                startDate,
                endDate,
                searchQuery.latitude(),
                searchQuery.longitude(),
                searchQuery.radiusKm()
        );
    }

    public List<Exercise> findCockpleRecommendedByDateRange(Member member, LocalDate startDate, LocalDate endDate) {
        return exerciseRepository.findCockpleRecommendedExercisesByDateRange(
                member.getId(), member.getGender(), member.getLevel(), member.getBirth().getYear(),
                startDate, endDate);
    }

    public List<Exercise> findFilteredRecommended(
            Member member,
            LocalDate startDate,
            LocalDate endDate,
            ExerciseRecommendationSearchCondition searchCondition) {
        return exerciseRepository.findFilteredRecommendedExercisesForCalendar(
                member.getId(), member.getBirth().getYear(), searchCondition, startDate, endDate);
    }
}
