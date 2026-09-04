package umc.cockple.demo.domain.member.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import umc.cockple.demo.domain.bookmark.domain.ExerciseBookmark;
import umc.cockple.demo.domain.bookmark.domain.PartyBookmark;
import umc.cockple.demo.domain.chat.domain.ChatMessage;
import umc.cockple.demo.domain.chat.domain.ChatRoomMember;
import umc.cockple.demo.domain.contest.domain.Contest;
import umc.cockple.demo.domain.exercise.domain.MemberExercise;
import umc.cockple.demo.domain.member.dto.MemberDetailInfoRequestDTO;
import umc.cockple.demo.domain.member.dto.UpdateProfileRequestDTO;
import umc.cockple.demo.domain.notification.domain.Notification;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.domain.member.enums.MemberStatus;
import umc.cockple.demo.global.common.BaseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    private String memberName;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private LocalDate birth;

    @Enumerated(EnumType.STRING)
    private Level level;

    private String nickname;

    @ColumnDefault("'ACTIVE'")
    @Enumerated(EnumType.STRING)
    private MemberStatus isActive;

    private LocalDateTime deletedAt;

    @Column(nullable = false)
    private Long socialId; // 카카오에서 받아온 고유id

    private String fcmToken;

    @ColumnDefault("0")
    @Column(nullable = false)
    private long tokenVersion;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Contest> contests = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Notification> notifications = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
    @Builder.Default
    private List<MemberKeyword> keywords = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
    @Builder.Default
    private List<MemberAddr> addresses = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
    @Builder.Default
    private List<MemberParty> memberParties = new ArrayList<>();

    // Member hard delete 시 참여 데이터 정리를 위한 ORM 생명주기 매핑이다.
    // 운동 참여의 생성·취소 책임은 Exercise가 가진다.
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
    @Builder.Default
    @Getter(AccessLevel.NONE)
    private List<MemberExercise> memberExercises = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
    @Builder.Default
    private List<ExerciseBookmark> exerciseBookmarks = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
    @Builder.Default
    private List<PartyBookmark> partyBookmarks = new ArrayList<>();

    @OneToOne(mappedBy = "member", cascade = CascadeType.ALL)
    private ProfileImg profileImg;

    @OneToMany(mappedBy = "sender")
    @Builder.Default
    private List<ChatMessage> chatMessages = new ArrayList<>();

    @OneToMany(mappedBy = "member")
    @Builder.Default
    private List<ChatRoomMember> chatRoomMembers = new ArrayList<>();


    public void updateProfileImg(ProfileImg newProfileImg) {
        this.profileImg = newProfileImg;
        // 양방향 관계 설정
        if (newProfileImg != null) {
            newProfileImg.setMember(this);
        }
    }


    public Optional<String> changeProfileImage(String imgKey) {
        if (this.profileImg != null) {
            if (this.profileImg.getImgKey().equals(imgKey)) {
                return Optional.empty(); // 동일 이미지 -> 변경 없음
            }
            String oldKey = this.profileImg.getImgKey();
            this.profileImg.updateProfile(imgKey); // UPDATE (version 증가 -> 낙관적 락 적용)
            return Optional.of(oldKey);
        }

        // 기존 이미지 없음 -> 새로 생성
        ProfileImg newProfileImg = ProfileImg.builder()
                .imgKey(imgKey)
                .build();
        updateProfileImg(newProfileImg);
        return Optional.empty();
    }

    public void updateMemberFirst(MemberDetailInfoRequestDTO requestDto, List<MemberKeyword> keywords) {
        this.memberName = requestDto.memberName();
        this.gender = requestDto.gender();
        this.birth = requestDto.birth();
        this.level = requestDto.level();
        this.keywords = keywords;
    }


    public void updateMember(UpdateProfileRequestDTO requestDto, List<MemberKeyword> keywords) {
        this.memberName = requestDto.memberName();
        this.birth = requestDto.birth();
        this.level = requestDto.level();
        this.keywords = keywords;
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

    public boolean isWithdrawn() {
        return this.isActive == MemberStatus.INACTIVE;
    }

    /**
     * 화면에 노출할 표시 이름
     */
    public String getDisplayName() {
        return memberName != null ? memberName : nickname;
    }

    /**
     * 온보딩(상세정보 입력) 완료 여부.
     */
    public boolean isProfileCompleted() {
        return this.memberName != null
                && this.gender != null
                && this.birth != null
                && this.level != null
        ;
    }

    public void withdraw() {
        this.isActive = MemberStatus.INACTIVE;
        this.deletedAt = LocalDateTime.now();
        this.fcmToken = null;
    }

    // FCM 토큰 업데이트 메서드
    public void updateFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    /**
     * 탈퇴한 회원의 재가입(계정 복원).
     * 탈퇴 시점의 프로필(이름/성별/생년월일/급수)은 그대로 보존되므로
     * 재가입 회원은 온보딩 없이 바로 홈으로 진입한다.
     */
    public void rejoin() {
        this.isActive = MemberStatus.ACTIVE;
        this.deletedAt = null;
    }
}
