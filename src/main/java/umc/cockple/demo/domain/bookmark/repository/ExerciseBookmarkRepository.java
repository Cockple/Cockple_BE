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

    /**
     * 찜한 운동 목록 조회 시 사용. 연관 엔티티를 한 번에 가져와 N+1을 제거한다.
     * - toOne(exercise, party, exerciseAddr)은 fetch join으로 즉시 로딩
     * - party의 역방향 @OneToOne(partyImg, chatRoom)은 LAZY여도 Hibernate가 party마다 즉시 조회하므로
     *   (eager라 batch로도 안 묶임) 함께 fetch join해 N+1을 제거한다.
     * - 컬렉션은 한 쿼리에 여러 bag을 fetch join할 수 없어(MultipleBagFetchException)
     *   levels만 fetch join하고, 나머지(memberExercises, guests)는 배치로 로딩된다.
     */
    @Query("""
            SELECT DISTINCT eb
            FROM ExerciseBookmark eb
            JOIN FETCH eb.exercise e
            JOIN FETCH e.party p
            LEFT JOIN FETCH e.exerciseAddr
            LEFT JOIN FETCH p.partyImg
            LEFT JOIN FETCH p.chatRoom
            LEFT JOIN FETCH p.levels
            WHERE eb.member = :member
            """)
    List<ExerciseBookmark> findAllByMemberWithDetails(@Param("member") Member member);


    @Query("""
            SELECT eb.exercise.id FROM ExerciseBookmark eb
            WHERE eb.member.id = :memberId 
            AND eb.exercise.id IN :exerciseIds
            """)
    List<Long> findAllExerciseIdsByMemberIdAndExerciseIds(
            @Param("memberId") Long memberId,
            @Param("exerciseIds") List<Long> exerciseIds
    );


    Optional<ExerciseBookmark> findFirstByMemberOrderByCreatedAtAsc(Member member);

}
