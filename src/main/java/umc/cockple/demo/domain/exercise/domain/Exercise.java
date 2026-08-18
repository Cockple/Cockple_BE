package umc.cockple.demo.domain.exercise.domain;

import jakarta.persistence.*;
import lombok.*;
import umc.cockple.demo.domain.exercise.enums.ExerciseMemberShipStatus;
import umc.cockple.demo.domain.exercise.service.command.model.ExerciseCreateCommand;
import umc.cockple.demo.domain.exercise.service.command.model.ExerciseUpdateAddressCommand;
import umc.cockple.demo.domain.exercise.service.command.model.ExerciseUpdateCommand;
import umc.cockple.demo.domain.game.domain.GameBoard;
import umc.cockple.demo.domain.game.domain.GameBoardMember;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.global.common.BaseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
public class Exercise extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(name = "addr_id")
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private ExerciseAddr exerciseAddr;

    @JoinColumn(name = "party_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Party party;

    @JoinColumn(name = "game_board_id", nullable = false, unique = true)
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true, optional = false)
    @Builder.Default
    private GameBoard gameBoard = GameBoard.create();

    @Column(nullable = false)
    private LocalDate date; // 운동 날짜

    @Column(nullable = false)
    private LocalTime startTime;

    private LocalTime endTime;

    @Column(nullable = false)
    private Integer maxCapacity;

    @Column(nullable = false)
    private Boolean partyGuestAccept;

    @Column(nullable = false)
    private Boolean outsideGuestAccept;

    private String notice;

    @OneToMany(mappedBy = "exercise", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Guest> addresses = new ArrayList<>();

    @OneToMany(mappedBy = "exercise", cascade = CascadeType.ALL)
    @Builder.Default
    private List<MemberExercise> memberExercises = new ArrayList<>();

    @OneToMany(mappedBy = "exercise", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Guest> guests = new ArrayList<>();

    public static Exercise create(ExerciseAddr exerciseAddr, ExerciseCreateCommand command) {
        return Exercise.builder()
                .exerciseAddr(exerciseAddr)
                .date(command.date())
                .startTime(command.startTime())
                .endTime(command.endTime())
                .maxCapacity(command.maxCapacity())
                .partyGuestAccept(command.partyGuestAccept())
                .outsideGuestAccept(command.outsideGuestAccept())
                .notice(command.notice())
                .build();
    }

    public void updateExerciseInfo(ExerciseUpdateCommand command) {
        if (command.date() != null) {
            this.date = command.date();
        }
        if (command.startTime() != null) {
            this.startTime = command.startTime();
        }
        if (command.endTime() != null) {
            this.endTime = command.endTime();
        }
        if (command.maxCapacity() != null) {
            this.maxCapacity = command.maxCapacity();
        }
        if (command.partyGuestAccept() != null) {
            this.partyGuestAccept = command.partyGuestAccept();
        }
        if (command.outsideGuestAccept() != null) {
            this.outsideGuestAccept = command.outsideGuestAccept();
        }
        if (command.notice() != null) {
            this.notice = command.notice();
        }
    }

    public void updateExerciseAddr(ExerciseUpdateAddressCommand command) {
        if (this.exerciseAddr != null && command != null) {
            this.exerciseAddr.updateAddress(command);
        }
    }

    public Integer getNowCapacity() {
        return memberExercises.size() + guests.size();
    }

    public boolean isAlreadyStarted() {
        LocalDateTime exerciseDateTime = LocalDateTime.of(this.date, this.startTime);
        return exerciseDateTime.isBefore(LocalDateTime.now());
    }

    /**
     * 연관관계 매핑 메서드
     */
    public void setParty(Party party) {
        this.party = party;
        if (party != null && !party.getExercises().contains(this)) {
            party.getExercises().add(this);
        }
    }

    public MemberExercise addParticipation(Member member, ExerciseMemberShipStatus status) {
        MemberExercise memberExercise = MemberExercise.create(member, this, status);
        this.memberExercises.add(memberExercise);
        this.gameBoard.addGameBoardMember(GameBoardMember.createFromMember(member, date));
        return memberExercise;
    }

    public void addGuest(Guest guest) {
        this.guests.add(guest);
        guest.setExercise(this);
        this.gameBoard.addGameBoardMember(GameBoardMember.createFromGuest(guest));
    }

    public void removeParticipation(MemberExercise memberExercise) {
        this.memberExercises.remove(memberExercise);
        this.gameBoard.removeGameBoardMember(memberExercise.getMember());
    }

    public void removeGuest(Guest guest) {
        this.guests.remove(guest);
        this.gameBoard.removeGameBoardMember(guest);
    }
}
