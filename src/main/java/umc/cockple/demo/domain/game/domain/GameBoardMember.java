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

import java.time.LocalDate;

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

    @JoinColumn(name = "game_board_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
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

    public static GameBoardMember createFromMember(Member member, LocalDate exerciseDate) {
        return GameBoardMember.builder()
                .member(member)
                .name(member.getMemberName())
                .gender(member.getGender())
                .level(member.getLevel())
                .ageGroup(AgeGroup.fromBirthDate(member.getBirth(), exerciseDate))
                .shuttlecockSubmitted(false)
                .participating(true)
                .gameCount(0)
                .build();
    }

    public static GameBoardMember createFromGuest(Guest guest) {
        return GameBoardMember.builder()
                .guest(guest)
                .name(guest.getGuestName())
                .gender(guest.getGender())
                .level(guest.getLevel())
                .ageGroup(null)
                .shuttlecockSubmitted(false)
                .participating(true)
                .gameCount(0)
                .build();
    }

    public void increaseGameCount() {
        this.gameCount++;
    }

    /**
     * member FK의 ON DELETE SET NULL과 guest FK의 ON DELETE CASCADE 때문에
     * MySQL CHECK 제약으로 같은 규칙을 중복 선언할 수 없어 애플리케이션 경계에서 검증한다.
     */
    @AssertTrue(message = "게임판 명단은 회원과 게스트를 동시에 참조할 수 없습니다.")
    private boolean isSourceReferenceValid() {
        return member == null || guest == null;
    }

    boolean originatesFrom(Member sourceMember) {
        return sameEntity(member, sourceMember);
    }

    boolean originatesFrom(Guest sourceGuest) {
        return sameEntity(guest, sourceGuest);
    }

    private boolean sameEntity(Member source, Member target) {
        return source == target
                || source != null && target != null
                && source.getId() != null && source.getId().equals(target.getId());
    }

    private boolean sameEntity(Guest source, Guest target) {
        return source == target
                || source != null && target != null
                && source.getId() != null && source.getId().equals(target.getId());
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
