package umc.cockple.demo.domain.exercise.domain;

import jakarta.persistence.*;
import lombok.*;
import umc.cockple.demo.domain.exercise.dto.ExerciseAddrUpdateCommand;
import umc.cockple.demo.domain.exercise.dto.ExerciseCreateCommand;
import umc.cockple.demo.domain.exercise.dto.ExerciseUpdateCommand;
import umc.cockple.demo.domain.exercise.dto.GuestInviteCommand;
import umc.cockple.demo.domain.member.domain.MemberExercise;
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

    public void reorderParticipantNumbers(int removedNum) {
        memberExercises.stream()
                .filter(me -> me.getParticipantNum() > removedNum)
                .forEach(MemberExercise::decrementParticipantNum);

        guests.stream()
                .filter(g -> g.getParticipantNum() > removedNum)
                .forEach(Guest::decrementParticipantNum);
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

    public Integer getNowCapacity() {
        return memberExercises.size() + guests.size();
    }

    public Integer calculateNextParticipantNumber() {
        return getNowCapacity() + 1;
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

    public void addParticipation(MemberExercise memberExercise) {
        this.memberExercises.add(memberExercise);
        memberExercise.setExercise(this);
    }

    public void addGuest(Guest guest) {
        this.guests.add(guest);
        guest.setExercise(this);
    }

    public void removeParticipation(MemberExercise memberExercise) {
        this.memberExercises.remove(memberExercise);
    }

    public void removeGuest(Guest guest) {
        this.guests.remove(guest);
    }


}
