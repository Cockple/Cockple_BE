package umc.cockple.demo.domain.exercise.domain;

import jakarta.persistence.*;
import lombok.*;
import umc.cockple.demo.domain.exercise.dto.ExerciseCreateCommand;
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
    private Integer nowCapacity;

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
                .nowCapacity(0)
                .partyGuestAccept(command.partyGuestAccept())
                .outsideGuestAccept(command.outsideGuestAccept())
                .notice(command.notice())
                .build();
    }

    public Integer removeParticipant(MemberExercise memberExercise) {
        int removedNum = memberExercise.getParticipantNum();
        removeFromParticipants(memberExercise);
        reorderParticipantNumbers(removedNum);

        return removedNum;
    }

    public boolean isAlreadyStarted() {
        LocalDateTime exerciseDateTime = LocalDateTime.of(this.date, this.startTime);
        return exerciseDateTime.isBefore(LocalDateTime.now());
    }

    public Integer calculateNextParticipantNumber() {
        return this.nowCapacity + 1;
    }

    private void reorderParticipantNumbers(int removedNum) {
        memberExercises.stream()
                .filter(me -> me.getParticipantNum() > removedNum)
                .forEach(MemberExercise::decrementParticipantNum);

        guests.stream()
                .filter(g -> g.getParticipantNum() > removedNum)
                .forEach(g -> g.decrementParticipantNum());
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
        this.nowCapacity++;
    }

    private void removeFromParticipants(MemberExercise memberExercise) {
        this.memberExercises.remove(memberExercise);
        this.nowCapacity--;
    }

    public void addGuest(Guest guest) {
        this.guests.add(guest);
        guest.setExercise(this);
        this.nowCapacity++;
    }
}
