package umc.cockple.demo.domain.game.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import lombok.*;
import umc.cockple.demo.domain.exercise.domain.Guest;
import umc.cockple.demo.domain.game.enums.AgeGroup;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.global.common.BaseEntity;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_game_board_member_board_member",
                columnNames = {"game_board_id", "member_id"}),
        @UniqueConstraint(
                name = "uk_game_board_member_board_guest",
                columnNames = {"game_board_id", "guest_id"})
})
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

    @JoinColumn(name = "member_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Member member;

    @JoinColumn(name = "guest_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Guest guest;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Level level;

    @Enumerated(EnumType.STRING)
    private AgeGroup ageGroup;

    @Column(nullable = false)
    private Boolean shuttlecockSubmitted;

    @Column(nullable = false)
    private Boolean participating;

    @Column(nullable = false)
    private Integer gameCount;

    public static GameBoardMember create(String name, Gender gender, Level level, AgeGroup ageGroup) {
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

    @AssertTrue(message = "게임판 명단은 회원과 게스트를 동시에 참조할 수 없습니다.")
    private boolean isSourceReferenceValid() {
        return member == null || guest == null;
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
