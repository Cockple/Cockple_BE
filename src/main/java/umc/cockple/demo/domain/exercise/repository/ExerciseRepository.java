package umc.cockple.demo.domain.exercise.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.cockple.demo.domain.exercise.domain.Exercise;

public interface ExerciseRepository extends JpaRepository<Exercise, Long> {
}
