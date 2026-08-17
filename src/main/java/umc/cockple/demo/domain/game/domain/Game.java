package umc.cockple.demo.domain.game.domain;

import jakarta.persistence.*;
import lombok.*;
import umc.cockple.demo.domain.game.enums.GameStatus;
import umc.cockple.demo.global.common.BaseEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
public class Game extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(name = "game_board_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private GameBoard gameBoard;

    @JoinColumn(name = "court_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Court court; // 대기 상태이거나 완료되어 코트에서 내려간 경우 null

    // 완료 이력에서 코트가 삭제되어도 코트 번호를 보여주기 위한 스냅샷
    private Integer courtNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameStatus status;

    private Integer waitingOrder; // 대기열 순서 (WAITING 상태에서만 유효)

    private LocalDateTime startedAt; // 코트 배치(게임 시작) 시각

    private LocalDateTime completedAt; // 게임 완료 시각

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<GamePlayer> players = new ArrayList<>();

    public static Game createWaiting(GameBoard gameBoard, int waitingOrder) {
        return Game.builder()
                .gameBoard(gameBoard)
                .status(GameStatus.WAITING)
                .waitingOrder(waitingOrder)
                .build();
    }

    public void changeCourt(Court court) {
        this.court = court;
        if (court != null) {
            this.courtNo = court.getCourtNo();
        }
    }

    public void start(Court court, LocalDateTime startedAt) {
        this.status = GameStatus.PLAYING;
        this.court = court;
        this.courtNo = court.getCourtNo();
        this.startedAt = startedAt;
        this.waitingOrder = null;
    }

    public void complete(LocalDateTime completedAt) {
        this.status = GameStatus.COMPLETED;
        if (this.court != null) {
            this.courtNo = this.court.getCourtNo();
        }
        this.court = null;
        this.completedAt = completedAt;
    }

    public void changeWaitingOrder(int waitingOrder) {
        this.waitingOrder = waitingOrder;
    }

    public void addPlayer(GamePlayer player) {
        this.players.add(player);
        player.setGame(this);
    }

    public void setGameBoard(GameBoard gameBoard) {
        this.gameBoard = gameBoard;
        if (gameBoard != null && !gameBoard.getGames().contains(this)) {
            gameBoard.getGames().add(this);
        }
    }
}
