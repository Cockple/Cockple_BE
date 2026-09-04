package umc.cockple.demo.domain.game.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import umc.cockple.demo.domain.game.domain.Game;
import umc.cockple.demo.domain.game.enums.GameStatus;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GameRepository extends JpaRepository<Game, Long>, GameRepositoryCustom {

    Optional<Game> findByCourtIdAndStatus(Long courtId, GameStatus status);

    boolean existsByCourtIdInAndStatus(Collection<Long> courtIds, GameStatus status);

    List<Game> findByGameBoardIdAndStatusOrderByWaitingOrderAsc(Long gameBoardId, GameStatus status);

    long countByGameBoardIdAndStatus(Long gameBoardId, GameStatus status);

    @Query("select (count(g) > 0) from Game g " +
            "join g.players p " +
            "where p.gameBoardMember.id = :gameBoardMemberId and g.status in :statuses")
    boolean existsByGameBoardMemberIdAndStatusIn(
            @Param("gameBoardMemberId") Long gameBoardMemberId,
            @Param("statuses") Collection<GameStatus> statuses);

    @Query("select distinct g from Game g " +
            "left join fetch g.court " +
            "left join fetch g.players p " +
            "left join fetch p.gameBoardMember gbm " +
            "left join fetch gbm.member m " +
            "left join fetch m.profileImg " +
            "where g.gameBoard.id = :gameBoardId and g.status in :statuses")
    List<Game> findByGameBoardIdAndStatusInWithPlayers(
            @Param("gameBoardId") Long gameBoardId,
            @Param("statuses") Collection<GameStatus> statuses);


    @Query("select g.id from Game g " +
            "where g.gameBoard.id = :gameBoardId and g.status = :status " +
            "and (:courtNo is null or g.courtNo = :courtNo) " +
            "and (:cursorTime is null " +
            "     or g.completedAt > :cursorTime " +
            "     or (g.completedAt = :cursorTime and g.id > :cursorId)) " +
            "order by g.completedAt asc, g.id asc")
    List<Long> findCompletedGameIds(
            @Param("gameBoardId") Long gameBoardId,
            @Param("status") GameStatus status,
            @Param("courtNo") Integer courtNo,
            @Param("cursorTime") LocalDateTime cursorTime,
            @Param("cursorId") Long cursorId,
            Pageable pageable);

    // 완료 게임 전용: 완료 게임은 court FK를 끊고 courtNo 스냅샷만 가지므로 court fetch 불필요
    @Query("select distinct g from Game g " +
            "left join fetch g.players p " +
            "left join fetch p.gameBoardMember " +
            "where g.id in :ids")
    List<Game> findByIdInWithPlayers(@Param("ids") Collection<Long> ids);

    @Query("select distinct g from Game g " +
            "left join fetch g.players p " +
            "left join fetch p.gameBoardMember " +
            "where g.status = :status and g.startedAt is not null and g.startedAt < :threshold")
    List<Game> findByStatusAndStartedAtBeforeWithPlayers(
            @Param("status") GameStatus status,
            @Param("threshold") LocalDateTime threshold);
}
