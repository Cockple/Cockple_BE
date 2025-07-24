package umc.cockple.demo.domain.bookmark.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import umc.cockple.demo.domain.bookmark.domain.ExerciseBookmark;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.member.domain.Member;

import java.util.List;
import java.util.Optional;

public interface ExerciseBookmarkRepository extends JpaRepository<ExerciseBookmark, Long> {

    Optional<ExerciseBookmark> findByMemberAndExercise(Member member, Exercise exercise);

    boolean existsByMemberAndExercise(Member member, Exercise exercise);

    List<ExerciseBookmark> findAllByMember(Member member);


    @Query("""
            SELECT eb.exercise.id FROM ExerciseBookmark eb
            WHERE eb.member.id = :memberId 
            AND eb.exercise.id IN :exerciseIds
            """)
    List<Long> findAllExerciseIdsByMemberIdAndExerciseIds(
            @Param("memberId") Long memberId,
            @Param("exerciseIds") List<Long> exerciseIds
    );

}
