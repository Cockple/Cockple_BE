package umc.cockple.demo.domain.exercise.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import umc.cockple.demo.domain.exercise.domain.Guest;

import java.util.List;

public interface GuestRepository extends JpaRepository<Guest, Long> {

    @Query("""
            SELECT g FROM Guest g 
            WHERE g.exercise.id = :exerciseId 
            ORDER BY g.createdAt ASC
            """)
    List<Guest> findByExerciseId(@Param("exerciseId") Long exerciseId);
}
