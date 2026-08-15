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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameStatus status;

    private Integer waitingOrder; // 대기열 순서 (WAITING 상태에서만 유효)

    private LocalDateTime startedAt; // 코트 배치(게임 시작) 시각

    private LocalDateTime completedAt; // 게임 완료 시각

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<GamePlayer> players = new ArrayList<>();

    public static Game createWaiting(Integer waitingOrder) {
        return Game.builder()
                .status(GameStatus.WAITING)
                .waitingOrder(waitingOrder)
                .build();
    }

    /**
     * 연관관계 매핑 메서드
     */
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
