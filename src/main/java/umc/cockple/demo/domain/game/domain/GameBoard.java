package umc.cockple.demo.domain.game.domain;

import jakarta.persistence.*;
import lombok.*;
import umc.cockple.demo.domain.exercise.domain.Guest;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.global.common.BaseEntity;

import java.util.ArrayList;
import java.util.List;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
public class GameBoard extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "gameBoard", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Court> courts = new ArrayList<>();

    @OneToMany(mappedBy = "gameBoard", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Game> games = new ArrayList<>();

    @OneToMany(mappedBy = "gameBoard", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<GameBoardMember> gameBoardMembers = new ArrayList<>();

    public static GameBoard create() {
        return GameBoard.builder().build();
    }

    /**
     * 연관관계 매핑 메서드
     */
    public void addCourt(Court court) {
        this.courts.add(court);
        court.setGameBoard(this);
    }

    public void addGame(Game game) {
        this.games.add(game);
        game.setGameBoard(this);
    }

    public void addGameBoardMember(GameBoardMember gameBoardMember) {
        this.gameBoardMembers.add(gameBoardMember);
        gameBoardMember.setGameBoard(this);
    }

    public void removeGameBoardMember(Member member) {
        removeGameBoardMember(gameBoardMembers.stream()
                .filter(gameBoardMember -> gameBoardMember.originatesFrom(member))
                .findFirst()
                .orElse(null));
    }

    public void removeGameBoardMember(Guest guest) {
        removeGameBoardMember(gameBoardMembers.stream()
                .filter(gameBoardMember -> gameBoardMember.originatesFrom(guest))
                .findFirst()
                .orElse(null));
    }

    private void removeGameBoardMember(GameBoardMember gameBoardMember) {
        if (gameBoardMember == null) {
            return;
        }
        this.gameBoardMembers.remove(gameBoardMember);
        gameBoardMember.setGameBoard(null);
    }
}
