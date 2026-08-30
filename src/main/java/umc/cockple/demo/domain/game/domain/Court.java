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

    private int courtNo; // 코트 번호 (표시/이동 기준)

    private String courtName; // 코트명 (생략 시 서비스에서 courtNo 기반 기본값 부여)

    public static Court create(GameBoard gameBoard, int courtNo, String courtName) {
        return Court.builder()
                .gameBoard(gameBoard)
                .courtNo(courtNo)
                .courtName(courtName)
                .build();
    }

    public void updateName(String courtName) {
        this.courtName = courtName;
    }

    /**
     * 코트 관리(순서 재배치 + 이름 변경)에서 courtNo와 이름을 함께 갱신한다.
     */
    public void update(int courtNo, String courtName) {
        this.courtNo = courtNo;
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
