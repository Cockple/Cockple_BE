package umc.cockple.demo.domain.party.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.domain.ExerciseAddr;
import umc.cockple.demo.domain.exercise.dto.ExerciseAddrCreateCommand;
import umc.cockple.demo.domain.exercise.dto.ExerciseCreateCommand;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberParty;
import umc.cockple.demo.domain.party.dto.PartyCreateCommand;
import umc.cockple.demo.global.common.BaseEntity;
import umc.cockple.demo.global.enums.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
public class Party extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String partyName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_addr_id")
    private PartyAddr partyAddr;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ParticipationType partyType;

    @Column(nullable = false)
    private Long ownerId;

    private Integer minAge;

    private Integer maxAge;

    @Column(nullable = false)
    private Integer price;

    @Column(nullable = false)
    private Integer joinPrice;

    @Column(nullable = false)
    private String designatedCock;

    @ColumnDefault("0")
    @Column(nullable = false)
    @Builder.Default
    private Integer exerciseCount = 0;

    private String content;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ActivityTime activityTime;

    @Builder.Default
    @OneToMany(mappedBy = "party", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PartyActiveDay> activeDays = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "party", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PartyKeyword> keywords = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "party", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PartyLevel> levels = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "party", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MemberParty> memberParties = new ArrayList<>();

    @OneToMany(mappedBy = "party", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Exercise> exercises = new ArrayList<>();

    @Setter
    @OneToOne(mappedBy = "party", cascade = CascadeType.ALL, orphanRemoval = true)
    private PartyImg partyImg;

    public static Party create(PartyCreateCommand command, PartyAddr addr, String imageUrl, Member owner) {
        Party party = Party.builder()
                .partyName(command.partyName())
                .partyType(ParticipationType.valueOf(command.partyType())) //enum으로 변환
                .ownerId(owner.getId())
                .minAge(command.minAge())
                .maxAge(command.maxAge())
                .price(command.price())
                .joinPrice(command.joinPrice())
                .designatedCock(command.designatedCock())
                .content(command.content())
                .activityTime(ActivityTime.valueOf(command.activityTime())) //enum으로 변환
                .partyAddr(addr)
                .build();

        party.addMember(MemberParty.createOwner(owner, party));

        if (imageUrl != null) {
            party.setPartyImg(PartyImg.create(imageUrl, party));
        }

        //다중 선택 정보를 추가하기 위한 메서드
        command.activityDay().forEach(day -> party.addActiveDay(ActiveDay.valueOf(day)));
        command.femaleLevel().forEach(level -> party.addLevel(Gender.FEMALE, Level.valueOf(level)));

        if (command.maleLevel() != null) {
            //여복일 경우 추가를 생략
            command.maleLevel().forEach(level -> party.addLevel(Gender.MALE, Level.valueOf(level)));
        }

        return party;
    }

    public void addMember(MemberParty memberParty) {
        this.memberParties.add(memberParty);
        memberParty.setParty(this);
    }

    public void addActiveDay(ActiveDay day) {
        PartyActiveDay partyActiveDay = PartyActiveDay.builder()
                .activeDay(day)
                .party(this)
                .build();
        this.activeDays.add(partyActiveDay);
    }

    public void addLevel(Gender gender, Level level) {
        PartyLevel partyLevel = PartyLevel.builder()
                .gender(gender)
                .level(level)
                .party(this)
                .build();
        this.levels.add(partyLevel);
    }

    public Exercise createExercise(ExerciseCreateCommand command, ExerciseAddrCreateCommand addrCommand) {
        ExerciseAddr exerciseAddr = ExerciseAddr.create(addrCommand);
        Exercise exercise = Exercise.create(this, exerciseAddr, command);

        // 🔥 setParty가 양방향 연관관계 설정
        this.exercises.add(exercise);
        exercise.setParty(this);

        this.exerciseCount = exercises.size();

        return exercise;
    }

}
