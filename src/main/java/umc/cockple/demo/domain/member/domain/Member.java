package umc.cockple.demo.domain.member.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import umc.cockple.demo.domain.bookmark.domain.ExerciseBookmark;
import umc.cockple.demo.domain.bookmark.domain.PartyBookmark;
import umc.cockple.demo.domain.contest.domain.Contest;
import umc.cockple.demo.domain.member.dto.UpdateProfileRequestDTO;
import umc.cockple.demo.domain.notification.domain.Notification;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.global.enums.MemberStatus;
import umc.cockple.demo.global.common.BaseEntity;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

import static umc.cockple.demo.domain.member.dto.CreateMemberAddrDTO.*;


@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String memberName;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(nullable = false)
    private LocalDate birth;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Level level;

    private String nickname;

    @ColumnDefault("'ACTIVE'")
    @Enumerated(EnumType.STRING)
    private MemberStatus isActive;

    private String refreshToken;


    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
    private List<Contest> contests = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Notification> notifications = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MemberKeyword> keywords = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MemberAddr> addresses = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MemberParty> memberParties = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MemberExercise> memberExercises = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExerciseBookmark> exerciseBookmarks = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PartyBookmark> partyBookmarks = new ArrayList<>();

    @OneToOne(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private ProfileImg profileImg;

    public void addParticipation(MemberExercise memberExercise) {
        this.memberExercises.add(memberExercise);
        memberExercise.setMember(this);
    }

    public void removeParticipation(MemberExercise memberExercise) {
        this.memberExercises.remove(memberExercise);
    }

    public void updateProfileImg(ProfileImg newProfileImg) {
        this.profileImg = newProfileImg;
        // 양방향 관계 설정
        if (newProfileImg != null) {
            newProfileImg.setMember(this);
        }
    }


    public void updateMember(UpdateProfileRequestDTO requestDto, List<MemberKeyword> keywords, ProfileImg img) {
        this.memberName = requestDto.memberName();
        this.birth = requestDto.birth();
        this.level = requestDto.level();
        this.keywords = keywords;
        this.profileImg = img;
    }

    public void updateMember(UpdateProfileRequestDTO requestDto, List<MemberKeyword> keywords) {
        this.memberName = requestDto.memberName();
        this.birth = requestDto.birth();
        this.level = requestDto.level();
        this.keywords = keywords;
    }

    public void addMemberParty(MemberParty memberParty) {
        this.memberParties.add(memberParty);
        memberParty.setMember(this);

    }

    public boolean hasDuplicateAddr(CreateMemberAddrRequestDTO requestDTO) {
        List<MemberAddr> addresses = this.getAddresses();
        return
                addresses.stream()
                        .anyMatch(addr ->
                                addr.getAddr1().equals(requestDTO.addr1()) &&
                                addr.getAddr2().equals(requestDTO.addr2()) &&
                                addr.getAddr3().equals(requestDTO.addr3()) &&
                                addr.getStreetAddr().equals(requestDTO.streetAddr()) &&
                                addr.getBuildingName().equals(requestDTO.buildingName()) &&
                                addr.getLatitude().equals(requestDTO.latitude()) &&
                                addr.getLongitude().equals(requestDTO.longitude())
                        );
    }

    public void withdraw() {
        this.isActive = MemberStatus.INACTIVE;
    }

    public int getAge(){
        return Period.between(birth, LocalDate.now()).getYears();
    }
}
