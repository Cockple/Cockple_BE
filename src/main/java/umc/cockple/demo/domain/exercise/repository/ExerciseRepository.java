package umc.cockple.demo.domain.exercise.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import umc.cockple.demo.domain.exercise.domain.Exercise;

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
}
