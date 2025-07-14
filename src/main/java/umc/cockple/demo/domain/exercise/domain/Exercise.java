package umc.cockple.demo.domain.exercise.domain;

import jakarta.persistence.*;
import lombok.*;
import umc.cockple.demo.domain.exercise.dto.ExerciseCreateCommand;
import umc.cockple.demo.domain.exercise.dto.GuestInviteCommand;
import umc.cockple.demo.domain.member.domain.Member;
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

    public static Exercise create(Party party, ExerciseAddr exerciseAddr, ExerciseCreateCommand command) {
        return Exercise.builder()
                .party(party)
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

    public MemberExercise addParticipant(Member member) {
        Integer participantNum = calculateNextParticipantNumber();
        MemberExercise memberExercise = MemberExercise.createParticipation(this, member, participantNum);
        addToParticipants(memberExercise);

        return memberExercise;
    }

    public Guest addGuest(GuestInviteCommand command) {
        Integer participantNum = calculateNextParticipantNumber();
        Guest guest = Guest.createForExercise(this, command, participantNum);
        addToGuests(guest);

        return guest;
    }

    private Integer calculateNextParticipantNumber() {
        return this.nowCapacity + 1;
    }

    public boolean isAlreadyStarted() {
        LocalDateTime exerciseDateTime = LocalDateTime.of(this.date, this.startTime);
        return exerciseDateTime.isBefore(LocalDateTime.now());
    }

    /**
     * 연관관계 매핑 메서드
     */
    private void addToParticipants(MemberExercise memberExercise) {
        this.memberExercises.add(memberExercise);
        memberExercise.setExercise(this);
        this.nowCapacity++;
    }

    private void addToGuests(Guest guest) {
        this.guests.add(guest);
        guest.setExercise(this);
        this.nowCapacity++;
    }

    public void setParty(Party party) {
        this.party = party;
        if (party != null && !party.getExercises().contains(this)) {
            party.getExercises().add(this);
        }
    }
}
