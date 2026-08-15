package umc.cockple.demo.domain.game.domain;

import jakarta.persistence.*;
import lombok.*;
import umc.cockple.demo.global.common.BaseEntity;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
public class GamePlayer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(name = "game_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Game game;

    @JoinColumn(name = "game_board_member_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private GameBoardMember gameBoardMember;

    @Column(nullable = false)
    private int playerOrder; // 게임 내 배치 순서 (팀 구성 기준, 0~3)

    public static GamePlayer create(GameBoardMember gameBoardMember, int playerOrder) {
        return GamePlayer.builder()
                .gameBoardMember(gameBoardMember)
                .playerOrder(playerOrder)
                .build();
    }

    /**
     * 연관관계 매핑 메서드
     */
    public void setGame(Game game) {
        this.game = game;
        if (game != null && !game.getPlayers().contains(this)) {
            game.getPlayers().add(this);
        }
    }
}
