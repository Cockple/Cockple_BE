package umc.cockple.demo.domain.exercise.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.party.dto.PartyExerciseInfoDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExerciseRepository extends JpaRepository<Exercise, Long> {

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
            ORDER BY e.date ASC, e.startTime ASC, p.partyName ASC
            """)
    List<Exercise> findByPartyIdsAndDateRangeOrderByLatest(
            @Param("partyIds") List<Long> myPartyIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

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
                e.id,
                COALESCE(me_count.member_count, 0) + COALESCE(g_count.guest_count, 0) as participant_count
            FROM exercise e
            LEFT JOIN (
                SELECT exercise_id, COUNT(*) as member_count 
                FROM member_exercise GROUP BY exercise_id
            ) me_count ON e.id = me_count.exercise_id
            LEFT JOIN (
                SELECT exercise_id, COUNT(*) as guest_count 
                FROM guest GROUP BY exercise_id
            ) g_count ON e.id = g_count.exercise_id
            WHERE e.id IN :exerciseIds
            """, nativeQuery = true)
    List<Object[]> findParticipantCounts(@Param("exerciseIds") List<Long> exerciseIds);
}
