package umc.cockple.demo.domain.game.domain;

import jakarta.persistence.*;
import lombok.*;
import umc.cockple.demo.global.common.BaseEntity;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
public class GameBoardMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(name = "game_board_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private GameBoard gameBoard;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Level level;

    private String ageGroup; // 연령대 (예: THIRTIES). 명세상 문자열, enum 정의는 명단관리 담당 결정 사항

    @Column(nullable = false)
    private Boolean shuttlecockSubmitted;

    @Column(nullable = false)
    private Boolean participating;

    @Column(nullable = false)
    private Integer gameCount;

    public static GameBoardMember create(String name, Gender gender, Level level, String ageGroup) {
        return GameBoardMember.builder()
                .name(name)
                .gender(gender)
                .level(level)
                .ageGroup(ageGroup)
                .shuttlecockSubmitted(false)
                .participating(true)
                .gameCount(0)
                .build();
    }

    public void increaseGameCount() {
        this.gameCount++;
    }

    /**
     * 연관관계 매핑 메서드
     */
    public void setGameBoard(GameBoard gameBoard) {
        this.gameBoard = gameBoard;
        if (gameBoard != null && !gameBoard.getGameBoardMembers().contains(this)) {
            gameBoard.getGameBoardMembers().add(this);
        }
    }
}
