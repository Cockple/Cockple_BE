package umc.cockple.demo.domain.exercise.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.repository.support.ExerciseMapSpatialSearchCondition;
import umc.cockple.demo.domain.party.dto.PartyExerciseInfoDTO;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExerciseRepository extends JpaRepository<Exercise, Long>, ExerciseRepositoryCustom {

    Optional<Exercise> findByGameBoardId(Long gameBoardId);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            UPDATE exercise e
            JOIN party p ON p.id = e.party_id
            SET e.game_host_id = p.owner_id
            WHERE e.party_id = :partyId
            AND e.game_host_id = :memberId
            """, nativeQuery = true)
    int restoreGameHostToPartyOwner(
            @Param("partyId") Long partyId,
            @Param("memberId") Long memberId);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            UPDATE exercise e
            JOIN party p ON p.id = e.party_id
            SET e.game_host_id = p.owner_id
            WHERE e.game_host_id = :memberId
            """, nativeQuery = true)
    int restoreGameHostsToPartyOwners(@Param("memberId") Long memberId);

    @Query("""
            SELECT e FROM Exercise e 
            JOIN FETCH e.party p 
            JOIN FETCH p.levels 
            WHERE e.id = :exerciseId
            """)
    Optional<Exercise> findByIdWithPartyLevels(@Param("exerciseId") Long exerciseId);

    @Query("""
            SELECT e FROM Exercise e 
            JOIN FETCH e.party p 
            JOIN FETCH e.exerciseAddr
            WHERE e.id = :exerciseId
            """)
    Optional<Exercise> findExerciseWithBasicInfo(@Param("exerciseId") Long exerciseId);

    // 여러 partyId에 해당하는 모든 예정된 운동들의 개수를 각각 세어서 반환
    @Query("""
            SELECT new umc.cockple.demo.domain.party.dto.PartyExerciseInfoDTO(e.party.id, COUNT(e)) 
            FROM Exercise e 
            WHERE e.party.id IN :partyIds AND (e.date > CURRENT_DATE OR (e.date = CURRENT_DATE AND e.startTime > CURRENT_TIME))
            GROUP BY e.party.id
            """)
    List<PartyExerciseInfoDTO> findTotalExerciseCountsByPartyIds(@Param("partyIds") List<Long> partyIds);

    //각 partyId에 해당하는 모든 예정된 운동들을 시간순 정렬을 하여 반환
    @Query("""
            SELECT e 
            FROM Exercise e
            WHERE e.party.id IN :partyIds AND (e.date > CURRENT_DATE OR (e.date = CURRENT_DATE AND e.startTime > CURRENT_TIME)) 
            ORDER BY e.date ASC, e.startTime ASC
            """)
    List<Exercise> findUpcomingExercisesByPartyIds(@Param("partyIds") List<Long> partyIds);

    @Query("""
            SELECT e FROM Exercise e 
            JOIN FETCH e.exerciseAddr addr
            WHERE e.party.id = :partyId 
            AND e.date BETWEEN :startDate AND :endDate
            ORDER BY e.date ASC, e.startTime ASC
            """)
    List<Exercise> findByPartyIdAndDateRange(
            @Param("partyId") Long partyId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query(value = """
            SELECT 
                e.id as exerciseId,
                COALESCE(me_count.member_count, 0) + COALESCE(g_count.guest_count, 0) as totalCount
            FROM exercise e
            LEFT JOIN (
                SELECT exercise_id, COUNT(*) as member_count 
                FROM member_exercise 
                GROUP BY exercise_id
            ) me_count ON e.id = me_count.exercise_id
            LEFT JOIN (
                SELECT exercise_id, COUNT(*) as guest_count 
                FROM guest 
                GROUP BY exercise_id
            ) g_count ON e.id = g_count.exercise_id
            WHERE e.party_id = :partyId 
            AND e.date BETWEEN :startDate AND :endDate
            """, nativeQuery = true)
    List<Object[]> findExerciseParticipantCounts(
            @Param("partyId") Long partyId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            SELECT e FROM Exercise e 
            JOIN FETCH e.memberExercises me
            JOIN FETCH e.exerciseAddr addr
            JOIN FETCH e.party p
            LEFT JOIN FETCH p.partyImg
            WHERE me.member.id = :memberId
            AND e.date BETWEEN :startDate AND :endDate
            ORDER BY e.date ASC, e.startTime ASC
            """)
    List<Exercise> findByMemberIdAndDateRange(
            @Param("memberId") Long memberId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
            SELECT e FROM Exercise e
            JOIN FETCH e.exerciseAddr addr
            JOIN FETCH e.party p
            LEFT JOIN FETCH p.partyImg
            WHERE e.party.id IN :partyIds
            AND (e.date > CURRENT_DATE OR (e.date = CURRENT_DATE AND e.startTime >= CURRENT_TIME))
            ORDER BY e.date ASC, e.startTime ASC
            """)
    List<Exercise> findRecentExercisesByPartyIds(@Param("partyIds") List<Long> partyIds, Pageable pageable);

    @Query("""
            SELECT e FROM Exercise e 
            JOIN FETCH e.party p
            JOIN FETCH e.exerciseAddr addr
            LEFT JOIN FETCH p.partyImg
            WHERE p.id IN :partyIds 
            AND e.date BETWEEN :startDate AND :endDate
            """)
    List<Exercise> findByPartyIdsAndDateRange(
            @Param("partyIds") List<Long> partyIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query(value = """
            SELECT 
                e.id as exerciseId,
                (SELECT COUNT(*) FROM member_exercise me WHERE me.exercise_id = e.id) + 
                (SELECT COUNT(*) FROM guest g WHERE g.exercise_id = e.id) as totalCount
            FROM exercise e
            WHERE e.id IN :exerciseIds
            AND e.date BETWEEN :startDate AND :endDate
            """, nativeQuery = true)
    List<Object[]> findExerciseParticipantCountsByExerciseIds(
            @Param("exerciseIds") List<Long> exerciseIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
            SELECT e From Exercise e
            JOIN FETCH e.party p
            JOIN FETCH e.exerciseAddr ea
            JOIN FETCH p.levels pl
            LEFT JOIN FETCH p.partyImg
            WHERE (e.date > CURRENT_DATE or (e.date = CURRENT_DATE AND e.startTime > CURRENT_TIME))
            AND NOT EXISTS (
                SELECT 1 FROM MemberParty mp
                WHERE mp.party.id = p.id
                AND mp.member.id = :memberId
                AND mp.member.isActive = 'ACTIVE'
            )
            AND NOT EXISTS (
                SELECT 1 FROM MemberExercise me
                WHERE me.exercise.id = e.id
                AND me.member.id = :memberId
            )
            AND (pl.gender = :gender AND pl.level = :level)
            AND (:birthYear >= p.minBirthYear AND :birthYear <= p.maxBirthYear)
            AND e.outsideGuestAccept = true
            """)
    List<Exercise> findExercisesByMemberIdAndLevelAndBirthYear(
            @Param("memberId") Long memberId,
            @Param("gender") Gender gender,
            @Param("level") Level level,
            @Param("birthYear") int birthYear);
           
    @Query("""
            SELECT e FROM Exercise e 
            JOIN FETCH e.memberExercises me
            JOIN FETCH e.exerciseAddr addr
            JOIN FETCH e.party p
            WHERE me.member.id = :memberId
            AND me.member.isActive = 'ACTIVE'
            """)
    Slice<Exercise> findMyExercisesWithPaging(@Param("memberId") Long memberId, Pageable pageable);

    @Query("""
            SELECT e FROM Exercise e 
            JOIN FETCH e.memberExercises me
            JOIN FETCH e.exerciseAddr addr
            JOIN FETCH e.party p
            WHERE me.member.id = :memberId
            AND me.member.isActive = 'ACTIVE'
            AND (e.date > CURRENT_DATE OR (e.date = CURRENT_DATE AND e.startTime > CURRENT_TIME))
            """)
    Slice<Exercise> findMyUpcomingExercisesWithPaging(@Param("memberId") Long memberId, Pageable pageable);

    @Query("""
            SELECT e FROM Exercise e 
            JOIN FETCH e.memberExercises me
            JOIN FETCH e.exerciseAddr addr
            JOIN FETCH e.party p
            WHERE me.member.id = :memberId
            AND me.member.isActive = 'ACTIVE'
            AND (e.date < CURRENT_DATE OR (e.date = CURRENT_DATE AND e.startTime <= CURRENT_TIME))
            """)
    Slice<Exercise> findMyCompletedExercisesWithPaging(@Param("memberId") Long memberId, Pageable pageable);

    @Query(value = """
            SELECT 
                e.id as exerciseId,
                (SELECT COUNT(*) FROM member_exercise me WHERE me.exercise_id = e.id) + 
                (SELECT COUNT(*) FROM guest g WHERE g.exercise_id = e.id) as totalCount
            FROM exercise e
            WHERE e.id IN :exerciseIds
            """, nativeQuery = true)
    List<Object[]> findExerciseParticipantCountsByExerciseIds(@Param("exerciseIds") List<Long> exerciseIds);

    @Query("""
            SELECT e FROM Exercise e
            JOIN FETCH e.exerciseAddr addr
            JOIN FETCH e.party p
            LEFT JOIN FETCH p.partyImg
            WHERE e.date = :date
            AND addr.buildingName = :buildingName
            AND addr.streetAddr = :streetAddr
            ORDER BY e.startTime ASC
            """)
    List<Exercise> findExercisesByBuildingAndDate(String buildingName, String streetAddr, LocalDate date);

    /*
     * 월간 지도 조회는 의도적으로 두 단계로 나눈다.
     * 1. native spatial query로 조건에 맞는 Exercise ID 후보군만 먼저 조회한다.
     * 2. 조회된 ID로 JPQL fetch join을 다시 수행해 실제 Exercise 엔티티를 로딩한다.
     *
     * native query에서 엔티티를 직접 조회하면 JPA fetch join과 연관 로딩 제어가 어려워지므로
     * 공간 검색 조건과 엔티티 로딩 책임을 분리한다.
     */
    default List<Exercise> findExercisesByMonthAndRadius(
            LocalDate startDate,
            LocalDate endDate,
            Double latitude,
            Double longitude,
            Double radiusKm) {
        ExerciseMapSpatialSearchCondition searchCondition =
                ExerciseMapSpatialSearchCondition.from(latitude, longitude, radiusKm);

        List<Long> exerciseIds = findExerciseIdsByMonthAndRadius(
                startDate,
                endDate,
                searchCondition.centerPointWkt(),
                searchCondition.boundingBoxWkt(),
                searchCondition.radiusKm());

        if (exerciseIds.isEmpty()) {
            return List.of();
        }

        return findExercisesByIdsForMonthlyMap(exerciseIds);
    }

    /*
     * 1단계 ID 후보군 조회 전용 쿼리다.
     *
     * 좌표 순서 계약:
     * - API/DTO 필드는 latitude, longitude 순서다.
     * - MySQL spatial WKT는 axis-order=long-lat와 함께 POINT(longitude latitude) 순서로 만든다.
     * - MBRWithin은 공간 인덱스를 타기 위한 bounding box 후보 필터다.
     * - ST_Distance_Sphere는 최종 원형 반경을 보장하는 정밀 거리 필터다.
     */
    @Query(value = """
            SELECT e.id
            FROM exercise_addr addr
            JOIN exercise e ON e.addr_id = addr.id
            WHERE e.date BETWEEN :startDate AND :endDate
            AND MBRWithin(
                addr.location,
                ST_GeomFromText(:boundingBoxWkt, 4326, 'axis-order=long-lat')
            )
            AND ST_Distance_Sphere(
                addr.location,
                ST_GeomFromText(:centerPointWkt, 4326, 'axis-order=long-lat')
            ) <= (:radiusKm * 1000.0)
            ORDER BY e.date ASC, e.start_time ASC
            """, nativeQuery = true)
    List<Long> findExerciseIdsByMonthAndRadius(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("centerPointWkt") String centerPointWkt,
            @Param("boundingBoxWkt") String boundingBoxWkt,
            @Param("radiusKm") Double radiusKm);

    /*
     * 2단계 엔티티 조회 전용 쿼리다.
     * 1단계에서 확정한 ID 후보군을 기준으로 Exercise와 월간 지도 응답에 필요한 주소를 로딩한다.
     */
    @Query("""
            SELECT e FROM Exercise e
            JOIN FETCH e.exerciseAddr addr
            WHERE e.id IN :exerciseIds
            ORDER BY e.date ASC, e.startTime ASC
            """)
    List<Exercise> findExercisesByIdsForMonthlyMap(@Param("exerciseIds") List<Long> exerciseIds);


    @Query("""
            SELECT e FROM Exercise e
            JOIN FETCH e.exerciseAddr addr
            JOIN FETCH e.party p
            LEFT JOIN FETCH p.partyImg
            WHERE e.date BETWEEN :startDate AND :endDate
            AND NOT EXISTS (
                SELECT 1 FROM MemberParty mp
                WHERE mp.party.id = p.id
                AND mp.member.id = :memberId
                AND mp.member.isActive = 'ACTIVE'
            )
            AND NOT EXISTS (
                SELECT 1 FROM MemberExercise me
                WHERE me.exercise.id = e.id
                AND me.member.id = :memberId
            )
            AND EXISTS (
                SELECT 1 FROM PartyLevel pl
                WHERE pl.party.id = p.id
                AND pl.gender = :gender
                AND pl.level = :level
            )
            AND (:birthYear >= p.minBirthYear AND :birthYear <= p.maxBirthYear)
            AND e.outsideGuestAccept = true
            """)
    List<Exercise> findCockpleRecommendedExercisesByDateRange(
            @Param("memberId") Long memberId,
            @Param("gender") Gender gender,
            @Param("level") Level level,
            @Param("birthYear") int birthYear,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
