package umc.cockple.demo.domain.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberExercise;

public interface MemberExerciseRepository extends JpaRepository<MemberExercise, Long> {

    boolean existsByExerciseAndMember(Exercise exercise, Member member);
}
