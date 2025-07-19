package umc.cockple.demo.domain.exercise.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.party.dto.PartyExerciseInfoDTO;

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
}
