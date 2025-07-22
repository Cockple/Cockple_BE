package umc.cockple.demo.domain.bookmark.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.cockple.demo.domain.bookmark.domain.ExerciseBookmark;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.member.domain.Member;

import java.util.Optional;

public interface ExerciseBookmarkRepository extends JpaRepository<ExerciseBookmark, Long> {

    Optional<ExerciseBookmark> findByMemberAndExercise(Member member, Exercise exercise);

    boolean existsByMemberAndExercise(Member member, Exercise exercise);
}
