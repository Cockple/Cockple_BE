package umc.cockple.demo.domain.game.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import umc.cockple.demo.domain.game.domain.GamePlayer;

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
}
