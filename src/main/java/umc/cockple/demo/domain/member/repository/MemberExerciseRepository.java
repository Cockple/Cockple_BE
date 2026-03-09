package umc.cockple.demo.domain.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberExercise;

import java.time.LocalDate;
import java.time.LocalTime;
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
            ORDER BY me.createdAt ASC
            """)
    List<MemberExercise> findByExerciseIdWithMemberAndProfile(
            @Param("exerciseId") Long exerciseId);

    @Modifying
    @Query("""
            DELETE FROM MemberExercise me
            WHERE me.member = :member
            AND (
                me.exercise.date > :today
                OR (me.exercise.date = :today AND me.exercise.startTime > :now)
            )
            """)
    void deleteFutureExercisesByMember(
            @Param("member") Member member,
            @Param("today") LocalDate today,
            @Param("now") LocalTime now);

    @Query("select me.exercise.id " +
            "from MemberExercise me " +
            "where me.member.id = :memberId and me.exercise.id in :exerciseIds")
    List<Long> findAllExerciseIdsByMemberAndExerciseIds(@Param("memberId") Long memberId,
                                                        @Param("exerciseIds") List<Long> exerciseIds);

    @Query("""
            SELECT me.member.id, MAX(e.date)
            FROM MemberExercise me
            JOIN me.exercise e
            WHERE me.member.id IN :memberIds
            AND e.party.id = :partyId
            AND e.date <= CURRENT_DATE
            GROUP BY me.member.id
            """)
    List<Object[]> findLastExerciseDateByMemberIdsAndPartyId(
            @Param("memberIds") List<Long> memberIds,
            @Param("partyId") Long partyId);
}
