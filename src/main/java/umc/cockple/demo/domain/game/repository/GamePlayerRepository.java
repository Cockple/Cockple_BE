package umc.cockple.demo.domain.game.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import umc.cockple.demo.domain.game.domain.GamePlayer;

import java.time.LocalDate;
import java.time.LocalTime;

public interface GamePlayerRepository extends JpaRepository<GamePlayer, Long> {

    @Query("""
            SELECT (COUNT(gamePlayer) > 0)
            FROM GamePlayer gamePlayer
            WHERE gamePlayer.gameBoardMember.gameBoard.id = :gameBoardId
              AND gamePlayer.gameBoardMember.member.id = :memberId
            """)
    boolean existsByMemberSource(
            @Param("gameBoardId") Long gameBoardId,
            @Param("memberId") Long memberId);

    @Query("""
            SELECT (COUNT(gamePlayer) > 0)
            FROM GamePlayer gamePlayer
            WHERE gamePlayer.gameBoardMember.gameBoard.id = :gameBoardId
              AND gamePlayer.gameBoardMember.guest.id = :guestId
            """)
    boolean existsByGuestSource(
            @Param("gameBoardId") Long gameBoardId,
            @Param("guestId") Long guestId);

    @Query(value = """
            SELECT COUNT(*)
            FROM game_player
            INNER JOIN game
                ON game.id = game_player.game_id
            INNER JOIN game_board_member
                ON game_board_member.id = game_player.game_board_member_id
            INNER JOIN exercise
                ON exercise.game_board_id = game_board_member.game_board_id
            WHERE game_board_member.member_id = :memberId
              AND game.status IN ('WAITING', 'PLAYING')
              AND (
                  exercise.date > :today
                  OR (exercise.date = :today AND exercise.start_time > :now)
              )
            """, nativeQuery = true)
    long countActiveFutureAssignmentsByMemberId(
            @Param("memberId") Long memberId,
            @Param("today") LocalDate today,
            @Param("now") LocalTime now);
}
