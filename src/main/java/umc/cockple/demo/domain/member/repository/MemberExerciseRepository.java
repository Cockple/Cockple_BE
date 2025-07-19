package umc.cockple.demo.domain.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberExercise;
import umc.cockple.demo.global.enums.MemberStatus;

import java.util.List;
import java.util.Optional;

public interface MemberExerciseRepository extends JpaRepository<MemberExercise, Long> {

    boolean existsByExerciseAndMember(Exercise exercise, Member member);

    Optional<MemberExercise> findByExerciseAndMember(Exercise exercise, Member member);

    @Query("""
            SELECT me FROM MemberExercise me
            JOIN FETCH me.member m
            LEFT JOIN FETCH m.profileImg mp
            WHERE me.exercise.id = :exerciseId
            AND m.isActive = :memberStatus
            ORDER BY me.createdAt ASC
            """)
    List<MemberExercise> findMemberParticipantsByExerciseId(
            @Param("exerciseId") Long exerciseId, @Param("memberStatus") MemberStatus memberStatus);
}
