package umc.cockple.demo.domain.exercise.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.cockple.demo.domain.exercise.domain.ExerciseAddr;

public interface ExerciseAddrRepository extends JpaRepository<ExerciseAddr, Long> {
}
