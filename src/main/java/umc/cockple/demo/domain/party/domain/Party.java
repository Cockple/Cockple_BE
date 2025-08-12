package umc.cockple.demo.domain.party.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import umc.cockple.demo.domain.bookmark.domain.PartyBookmark;
import umc.cockple.demo.domain.chat.domain.ChatRoom;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.domain.ExerciseAddr;
import umc.cockple.demo.domain.exercise.dto.ExerciseCreateDTO;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberParty;
import umc.cockple.demo.domain.party.dto.PartyCreateDTO;
import umc.cockple.demo.domain.party.dto.PartyUpdateDTO;
import umc.cockple.demo.domain.party.enums.ActiveDay;
import umc.cockple.demo.domain.party.enums.ActivityTime;
import umc.cockple.demo.domain.party.enums.ParticipationType;
import umc.cockple.demo.domain.party.enums.PartyStatus;
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

    @Column(nullable = false)
    private Integer minBirthYear;

    @Column(nullable = false)
    private Integer maxBirthYear;

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

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PartyStatus status = PartyStatus.ACTIVE;

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

    @OneToOne(mappedBy = "party", cascade = CascadeType.ALL, orphanRemoval = true)
    private PartyImg partyImg;

    @Builder.Default
    @OneToMany(mappedBy = "party", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PartyBookmark> partyBookmarks = new ArrayList<>();

    @OneToOne(mappedBy = "party", cascade = CascadeType.ALL, orphanRemoval = true)
    private ChatRoom chatRoom;


    public static Party create(PartyCreateDTO.Command command, PartyAddr addr, Member owner) {
        Party party = Party.builder()
                .partyName(command.partyName())
                .partyType(command.partyType())
                .ownerId(owner.getId())
                .minBirthYear(command.minBirthYear())
                .maxBirthYear(command.maxBirthYear())
                .price(command.price())
                .joinPrice(command.joinPrice())
                .designatedCock(command.designatedCock())
                .content(command.content())
                .activityTime(command.activityTime())
                .partyAddr(addr)
                .build();

        party.addMember(MemberParty.createOwner(owner, party));

        String imageKey = command.imgKey();
        if (imageKey != null && !imageKey.isBlank()) {
            party.setPartyImg(PartyImg.create(imageKey, party));
        }

        //다중 선택 정보를 추가하기 위한 메서드
        command.activityDay().forEach(party::addActiveDay);
        command.femaleLevel().forEach(level -> party.addLevel(Gender.FEMALE, level));
        if (command.maleLevel() != null) {
            //여복일 경우 추가를 생략
            command.maleLevel().forEach(level -> party.addLevel(Gender.MALE, level));
        }

        return party;
    }

    public Exercise createExercise(ExerciseCreateDTO.Command command, ExerciseCreateDTO.AddrCommand addrCommand) {
        ExerciseAddr exerciseAddr = ExerciseAddr.create(addrCommand);
        return Exercise.create(exerciseAddr, command);
    }

    public boolean isAgeValid(Member member){
        int birthYear = member.getBirth().getYear();

        if(minBirthYear != null && birthYear < minBirthYear){
            return false;
        }if(maxBirthYear != null && birthYear > maxBirthYear){
            return false;
        }
        return true;
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

    public void addKeyword(Keyword keyword) {
        PartyKeyword partyKeyword = PartyKeyword.builder()
                .keyword(keyword)
                .party(this)
                .build();
        this.keywords.add(partyKeyword);
    }

    public void addLevel(Gender gender, Level level) {
        PartyLevel partyLevel = PartyLevel.builder()
                .gender(gender)
                .level(level)
                .party(this)
                .build();
        this.levels.add(partyLevel);
    }

    public void addExercise(Exercise exercise) {
        this.exercises.add(exercise);
        exercise.setParty(this);

        this.exerciseCount = exercises.size();
    }

    protected void setPartyImg(PartyImg partyImg) {
        this.partyImg = partyImg;
        partyImg.setParty(this);
    }

    public void removeExercise(Exercise exercise) {
        this.exercises.remove(exercise);

        this.exerciseCount = exercises.size();
    }

    public void removeKeyword(PartyKeyword partyKeyword) {
        this.keywords.remove(partyKeyword);
    }

    public void delete() {
        this.status = PartyStatus.INACTIVE;

        // orphanRemoval=true 옵션에 의해 관계를 제거하면 DB에서도 자동으로 삭제됨
        this.exercises.clear();
        this.memberParties.clear();
        this.partyBookmarks.clear();
    }

    public void update(PartyUpdateDTO.Request request) {
        if (request.activityDay() != null) {
            this.activeDays.clear(); //연결관계 끊기 (리스트이기에 clear로 전체 삭제)
            request.activityDay().stream()
                    .map(ActiveDay::fromKorean)
                    .forEach(this::addActiveDay);
        }
        if (request.activityTime() != null) {
            this.activityTime = ActivityTime.fromKorean(request.activityTime());
        }
        if (request.designatedCock() != null) {
            this.designatedCock = request.designatedCock();
        }
        if (request.joinPrice() != null) {
            this.joinPrice = request.joinPrice();
        }
        if (request.price() != null) {
            this.price = request.price();
        }
        if (request.content() != null) {
            this.content = request.content();
        }
        if (request.imgKey() != null) {
            if (request.imgKey().isEmpty()) {
                this.partyImg = null; //연결관계 끊기 (단일 객체기에 null)
            } else {
                if (this.partyImg != null) {
                    this.partyImg.updateKey(request.imgKey());
                } else {
                    this.setPartyImg(PartyImg.create(request.imgKey(), this));
                }
            }
        }
        if (request.keyword() != null) {
            this.keywords.clear();
            request.keyword().stream()
                    .map(Keyword::fromKorean)
                    .forEach(this::addKeyword);
        }
    }
}
