package umc.cockple.demo.domain.game.domain;

import jakarta.persistence.*;
import lombok.*;
import umc.cockple.demo.global.common.BaseEntity;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
public class Court extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(name = "game_board_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private GameBoard gameBoard;

    @Column(nullable = false)
    private Integer courtNo; // 코트 번호 (표시/이동 기준)

    private String courtName; // 코트명 (생략 시 서비스에서 courtNo 기반 기본값 부여)

    public static Court create(Integer courtNo, String courtName) {
        return Court.builder()
                .courtNo(courtNo)
                .courtName(courtName)
                .build();
    }

    public void updateName(String courtName) {
        this.courtName = courtName;
    }

    /**
     * 연관관계 매핑 메서드
     */
    public void setGameBoard(GameBoard gameBoard) {
        this.gameBoard = gameBoard;
        if (gameBoard != null && !gameBoard.getCourts().contains(this)) {
            gameBoard.getCourts().add(this);
        }
    }
}
