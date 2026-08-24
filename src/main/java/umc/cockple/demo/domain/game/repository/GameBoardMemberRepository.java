package umc.cockple.demo.domain.game.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import umc.cockple.demo.domain.game.domain.GameBoardMember;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GameBoardMemberRepository extends JpaRepository<GameBoardMember, Long>, GameBoardMemberRepositoryCustom {

    Optional<GameBoardMember> findByIdAndGameBoardId(Long id, Long gameBoardId);

    List<GameBoardMember> findByGameBoardIdAndIdIn(Long gameBoardId, Collection<Long> ids);

    List<GameBoardMember> findByGameBoardIdOrderByIdAsc(Long gameBoardId);

    long countByGameBoardId(Long gameBoardId);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            DELETE game_board_member
            FROM game_board_member
            INNER JOIN exercise
                ON exercise.game_board_id = game_board_member.game_board_id
            WHERE game_board_member.member_id = :memberId
              AND (
                  exercise.date > :today
                  OR (exercise.date = :today AND exercise.start_time > :now)
              )
            """, nativeQuery = true)
    int deleteFutureByMemberId(
            @Param("memberId") Long memberId,
            @Param("today") LocalDate today,
            @Param("now") LocalTime now);
}
