package umc.cockple.demo.domain.exercise.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.party.dto.PartyExerciseInfoDTO;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExerciseRepository extends JpaRepository<Exercise, Long>, ExerciseRepositoryCustom {

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

    @Query("""
            SELECT e FROM Exercise e
            JOIN FETCH e.exerciseAddr addr
            WHERE e.date BETWEEN :startDate AND :endDate
            AND (
                (6371 * acos(
                    LEAST(1.0, cos(radians(:latitude)) * cos(radians(addr.latitude)) *
                    cos(radians(addr.longitude) - radians(:longitude)) +
                    sin(radians(:latitude)) * sin(radians(addr.latitude)))
                )) <= :radiusKm
                OR (addr.latitude = :latitude AND addr.longitude = :longitude)
            )
            ORDER BY e.date ASC, e.startTime ASC
            """)
    List<Exercise> findExercisesByMonthAndRadius(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate end,
            @Param("latitude") Double latitude,
            @Param("longitude") Double longitude,
            @Param("radiusKm") Integer radiusKm);


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
