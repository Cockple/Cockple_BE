package umc.cockple.demo.domain.bookmark.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.cockple.demo.domain.bookmark.domain.ExerciseBookmark;

public interface ExerciseBookmarkRepository extends JpaRepository<ExerciseBookmark, Long> {
}
