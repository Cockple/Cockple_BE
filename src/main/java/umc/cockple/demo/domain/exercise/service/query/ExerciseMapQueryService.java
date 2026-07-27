package umc.cockple.demo.domain.exercise.service.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.exercise.converter.query.ExerciseMapQueryMapper;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.dto.map.ExerciseBuildingDetailDTO;
import umc.cockple.demo.domain.exercise.dto.map.ExerciseMapBuildingsDTO;
import umc.cockple.demo.domain.bookmark.service.query.lookup.ExerciseBookmarkLookupService;
import umc.cockple.demo.domain.exercise.service.query.model.ExerciseMapSearchQuery;
import umc.cockple.demo.domain.exercise.service.support.reader.ExerciseReader;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberAddr;
import umc.cockple.demo.domain.member.service.query.lookup.MemberLookupService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ExerciseMapQueryService {

    private final ExerciseReader exerciseReader;
    private final ExerciseBookmarkLookupService exerciseBookmarkLookupService;
    private final MemberLookupService memberLookupService;
    private final ExerciseMapQueryMapper exerciseMapMapper;

    public ExerciseBuildingDetailDTO.Response getBuildingExerciseDetails(
            String buildingName, String streetAddr, LocalDate date, Long memberId) {

        log.info("건물 운동 상세 조회 시작 - 건물: {}, 주소: {}, 날짜: {}", buildingName, streetAddr, date);

        List<Exercise> exercises = exerciseReader.findByBuildingAndDate(buildingName, streetAddr, date);

        if (exercises.isEmpty()) {
            log.info("건물에 운동이 존재하지 않습니다. - 건물: {}, 주소: {}, 날짜: {}", buildingName, streetAddr, date);
            return exerciseMapMapper.toEmptyBuildingDetailResponse(buildingName, date);
        }

        List<Long> exerciseIds = getExerciseIds(exercises);
        Map<Long, Boolean> bookmarkStatus = exerciseBookmarkLookupService.getBookmarkStatus(memberId, exerciseIds);

        log.info("건물 운동 상세 조회 종료 - 건물: {}, 주소: {}, 날짜: {}, 결과: {}", buildingName, streetAddr, date, exerciseIds.size());

        return exerciseMapMapper.toBuildingDetailResponse(exercises, buildingName, bookmarkStatus, date);
    }

    public ExerciseMapBuildingsDTO.Response getExerciseMapCalendarSummary(
            ExerciseMapSearchQuery query, Long memberId) {

        log.info("월간 운동 캘린더 요약 조회 시작 - 날짜: {}, 중심: ({}, {}), 반경: {}km",
                query.date(), query.latitude(), query.longitude(), query.radiusKm());

        Member member = memberLookupService.findWithAddressesOrThrow(memberId);
        MemberAddr mainAddr = memberLookupService.findMainAddressOrThrow(member);
        ExerciseMapSearchQuery searchQuery =
                query.withFallbackLocation(mainAddr.getLatitude(), mainAddr.getLongitude());

        DateRange dateRange = DateRange.calculateMonthlyStartAndEnd(query.date());

        List<Exercise> exercises = exerciseReader.findByMonthAndRadius(dateRange.start(), dateRange.end(), searchQuery);

        Map<LocalDate, List<ExerciseMapBuildingsDTO.BuildingInfo>> dailyBuildings =
                groupExercisesByDateAndBuilding(exercises);

        log.info("월간 운동 캘린더 요약 조회 완료 - 조회된 운동 수: {}", exercises.size());

        return exerciseMapMapper.toMapCalendarSummaryResponse(
                dateRange.start().getYear(), dateRange.start().getMonthValue(),
                searchQuery.latitude(), searchQuery.longitude(), searchQuery.radiusKm(), dailyBuildings);
    }

    private static List<Long> getExerciseIds(List<Exercise> exercises) {
        return exercises.stream().map(Exercise::getId).toList();
    }

    private Map<LocalDate, List<ExerciseMapBuildingsDTO.BuildingInfo>> groupExercisesByDateAndBuilding(List<Exercise> exercises) {
        Map<LocalDate, List<Exercise>> exercisesByDate = exercises.stream()
                .collect(Collectors.groupingBy(Exercise::getDate));

        return exercisesByDate.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> createBuildingSummariesForDate(entry.getValue()),
                        (existing, replacement) -> existing,
                        TreeMap::new
                ));
    }

    private List<ExerciseMapBuildingsDTO.BuildingInfo> createBuildingSummariesForDate(List<Exercise> dayExercises) {
        Map<BuildingKey, List<Exercise>> exercisesByBuilding = dayExercises.stream()
                .collect(Collectors.groupingBy(this::createBuildingKey));

        return exercisesByBuilding.keySet().stream()
                .map(entry -> exerciseMapMapper.toBuildingSummary(
                        entry.name(), entry.address(), entry.latitude(), entry.longitude())
                )
                .toList();
    }

    private BuildingKey createBuildingKey(Exercise exercise) {
        var addr = exercise.getExerciseAddr();

        return new BuildingKey(
                addr.getBuildingName(),
                addr.getStreetAddr(),
                addr.getLatitude().doubleValue(),
                addr.getLongitude().doubleValue()
        );
    }

    private record DateRange(LocalDate start, LocalDate end) {
        private static DateRange calculateMonthlyStartAndEnd(LocalDate date) {
            LocalDate targetDate = (date != null) ? date : LocalDate.now();

            LocalDate start = targetDate.withDayOfMonth(1);
            int lastDay = targetDate.lengthOfMonth();
            LocalDate end = targetDate.withDayOfMonth(lastDay);

            return new DateRange(start, end);
        }
    }

    private record BuildingKey(
            String name,
            String address,
            Double latitude,
            Double longitude
    ) {
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;

            BuildingKey that = (BuildingKey) obj;
            return Objects.equals(name, that.name) &&
                    Objects.equals(address, that.address);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, address);
        }
    }
}
