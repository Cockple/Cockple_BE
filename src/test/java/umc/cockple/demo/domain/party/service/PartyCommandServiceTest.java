package umc.cockple.demo.domain.party.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import umc.cockple.demo.domain.chat.service.ChatRoomService;
import umc.cockple.demo.domain.file.service.FileService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberParty;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.exception.MemberException;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.notification.service.NotificationCommandService;
import umc.cockple.demo.domain.party.converter.PartyConverter;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.domain.PartyAddr;
import umc.cockple.demo.domain.party.domain.PartyInvitation;
import umc.cockple.demo.domain.party.domain.PartyJoinRequest;
import umc.cockple.demo.domain.party.dto.*;
import umc.cockple.demo.domain.party.enums.ParticipationType;
import umc.cockple.demo.domain.party.enums.PartyStatus;
import umc.cockple.demo.domain.party.enums.RequestAction;
import umc.cockple.demo.domain.party.enums.RequestStatus;
import umc.cockple.demo.domain.party.events.PartyDeletedEvent;
import umc.cockple.demo.domain.party.events.PartyMemberJoinedEvent;
import umc.cockple.demo.domain.party.exception.PartyErrorCode;
import umc.cockple.demo.domain.party.exception.PartyException;
import umc.cockple.demo.domain.party.repository.PartyAddrRepository;
import umc.cockple.demo.domain.party.repository.PartyInvitationRepository;
import umc.cockple.demo.domain.party.repository.PartyJoinRequestRepository;
import umc.cockple.demo.domain.party.repository.PartyRepository;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.global.enums.Role;
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PartyCommandServiceTest {

    @InjectMocks
    private PartyCommandServiceImpl partyCommandService;

    @Mock
    private PartyRepository partyRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private NotificationCommandService notificationCommandService;
    @Mock
    private PartyAddrRepository partyAddrRepository;
    @Mock
    private MemberPartyRepository memberPartyRepository;
    @Mock
    private ChatRoomService chatRoomService;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @Mock
    private PartyJoinRequestRepository partyJoinRequestRepository;
    @Mock
    private PartyInvitationRepository partyInvitationRepository;
    @Mock
    private FileService fileService;

    private PartyConverter partyConverter;

    @BeforeEach
    void setUp() {
        partyConverter = new PartyConverter(fileService);
        ReflectionTestUtils.setField(partyCommandService, "partyConverter", partyConverter);
    }

    @Nested
    @DisplayName("leaveParty")
    class LeaveParty {

        @Test
        @DisplayName("성공 - 일반 멤버가 모임을 탈퇴한다")
        void success_leaveParty() {
            // given
            Long partyId = 1L;
            Long memberId = 10L;

            PartyAddr addr = PartyFixture.createPartyAddr("서울", "강남");
            Member owner = MemberFixture.createMember("모임장", Gender.MALE, Level.A, 1L);
            ReflectionTestUtils.setField(owner, "id", 1L);
            Party party = PartyFixture.createParty("탈퇴 테스트 모임", owner.getId(), addr);
            ReflectionTestUtils.setField(party, "id", partyId);

            Member member = MemberFixture.createMember("일반멤버", Gender.MALE, Level.A, 10L);
            ReflectionTestUtils.setField(member, "id", memberId);

            MemberParty memberParty = MemberFixture.createMemberParty(party, member, Role.PARTY_MEMBER);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
            given(memberPartyRepository.findByPartyAndMember(party, member)).willReturn(Optional.of(memberParty));

            // when
            partyCommandService.leaveParty(partyId, memberId);

            // then
            verify(memberPartyRepository).delete(memberParty);
            verify(chatRoomService).leavePartyChatRoom(partyId, memberId);
            verify(applicationEventPublisher).publishEvent(any(PartyMemberJoinedEvent.class));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 모임인 경우 PARTY_NOT_FOUND 예외가 발생한다")
        void fail_leaveParty_partyNotFound() {
            // given
            given(partyRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> partyCommandService.leaveParty(999L, 1L))
                    .isInstanceOf(PartyException.class)
                    .satisfies(
                            e -> assertThat(((PartyException) e).getCode()).isEqualTo(PartyErrorCode.PARTY_NOT_FOUND));
        }

        @Test
        @DisplayName("실패 - 삭제된 모임인 경우 PARTY_IS_DELETED 예외가 발생한다")
        void fail_leaveParty_partyDeleted() {
            // given
            Long partyId = 1L;
            PartyAddr addr = PartyFixture.createPartyAddr("서울", "강남");
            Party party = PartyFixture.createParty("삭제된 모임", 1L, addr);
            party.delete();

            Member member = MemberFixture.createMember("일반멤버", Gender.MALE, Level.A, 1L);
            ReflectionTestUtils.setField(member, "id", 1L);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(memberRepository.findById(1L)).willReturn(Optional.of(member));

            // when & then
            assertThatThrownBy(() -> partyCommandService.leaveParty(partyId, 1L))
                    .isInstanceOf(PartyException.class)
                    .satisfies(
                            e -> assertThat(((PartyException) e).getCode()).isEqualTo(PartyErrorCode.PARTY_IS_DELETED));
        }

        @Test
        @DisplayName("실패 - 모임장이 탈퇴하려 할 경우 INVALID_ACTION_FOR_OWNER 예외가 발생한다")
        void fail_leaveParty_isOwner() {
            // given
            Long partyId = 1L;
            Long ownerId = 1L;

            PartyAddr addr = PartyFixture.createPartyAddr("서울", "강남");
            Member owner = MemberFixture.createMember("모임장", Gender.MALE, Level.A, 1L);
            ReflectionTestUtils.setField(owner, "id", ownerId);
            Party party = PartyFixture.createParty("탈퇴 테스트 모임", owner.getId(), addr);
            ReflectionTestUtils.setField(party, "id", partyId);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(memberRepository.findById(ownerId)).willReturn(Optional.of(owner));

            // when & then
            assertThatThrownBy(() -> partyCommandService.leaveParty(partyId, ownerId))
                    .isInstanceOf(PartyException.class)
                    .satisfies(e -> assertThat(((PartyException) e).getCode())
                            .isEqualTo(PartyErrorCode.INVALID_ACTION_FOR_OWNER));
        }

        @Test
        @DisplayName("실패 - 부모임장이 탈퇴하려 할 경우 INVALID_ACTION_FOR_SUBOWNER 예외가 발생한다")
        void fail_leaveParty_isSubOwner() {
            // given
            Long partyId = 1L;
            Long subManagerId = 2L;

            PartyAddr addr = PartyFixture.createPartyAddr("서울", "강남");
            Member owner = MemberFixture.createMember("모임장", Gender.MALE, Level.A, 1L);
            ReflectionTestUtils.setField(owner, "id", 1L);
            Party party = PartyFixture.createParty("탈퇴 테스트 모임", owner.getId(), addr);
            ReflectionTestUtils.setField(party, "id", partyId);

            Member subManager = MemberFixture.createMember("부모임장", Gender.MALE, Level.A, 2L);
            ReflectionTestUtils.setField(subManager, "id", subManagerId);

            MemberParty subManagerParty = MemberFixture.createMemberParty(party, subManager, Role.PARTY_SUBMANAGER);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(memberRepository.findById(subManagerId)).willReturn(Optional.of(subManager));
            given(memberPartyRepository.findByPartyIdAndRole(partyId, Role.PARTY_SUBMANAGER))
                    .willReturn(Optional.of(subManagerParty));

            // when & then
            assertThatThrownBy(() -> partyCommandService.leaveParty(partyId, subManagerId))
                    .isInstanceOf(PartyException.class)
                    .satisfies(e -> assertThat(((PartyException) e).getCode())
                            .isEqualTo(PartyErrorCode.INVALID_ACTION_FOR_SUBOWNER));
        }

        @Test
        @DisplayName("실패 - 모임 멤버가 아닌 경우 NOT_MEMBER 예외가 발생한다")
        void fail_leaveParty_notMember() {
            // given
            Long partyId = 1L;
            Long memberId = 10L;

            PartyAddr addr = PartyFixture.createPartyAddr("서울", "강남");
            Party party = PartyFixture.createParty("탈퇴 테스트 모임", 1L, addr);
            ReflectionTestUtils.setField(party, "id", partyId);
            Member member = MemberFixture.createMember("외부인", Gender.MALE, Level.A, 10L);
            ReflectionTestUtils.setField(member, "id", memberId);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
            given(memberPartyRepository.findByPartyAndMember(party, member)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> partyCommandService.leaveParty(partyId, memberId))
                    .isInstanceOf(PartyException.class)
                    .satisfies(e -> assertThat(((PartyException) e).getCode()).isEqualTo(PartyErrorCode.NOT_MEMBER));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 회원인 경우 MEMBER_NOT_FOUND 예외가 발생한다")
        void fail_leaveParty_memberNotFound() {
            // given
            Long partyId = 1L;
            PartyAddr addr = PartyFixture.createPartyAddr("서울", "강남");
            Party party = PartyFixture.createParty("모임명", 1L, addr);
            ReflectionTestUtils.setField(party, "id", partyId);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(memberRepository.findById(10L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> partyCommandService.leaveParty(partyId, 10L))
                    .isInstanceOf(MemberException.class)
                    .satisfies(e -> assertThat(((MemberException) e).getCode()).isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("createJoinRequest")
    class CreateJoinRequest {

        @Test
        @DisplayName("성공 - 사용자가 특정 모임에 가입 신청을 성공적으로 완료한다")
        void success_createJoinRequest() {
            // given
            Long partyId = 1L;
            Long memberId = 1L;

            PartyAddr addr = PartyFixture.createPartyAddr("서울", "강남");
            Party party = PartyFixture.createParty("가입 신청 모임", 10L, addr);
            Member member = MemberFixture.createMember("지원자", Gender.MALE, Level.B, 1L, LocalDate.of(1995, 1, 1));
            ReflectionTestUtils.setField(member, "id", memberId);

            given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(memberPartyRepository.existsByPartyAndMember(party, member)).willReturn(false);
            given(partyJoinRequestRepository.existsByPartyAndMemberAndStatus(party, member, RequestStatus.PENDING)).willReturn(false);
            given(partyJoinRequestRepository.save(any(PartyJoinRequest.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            PartyJoinCreateDTO.Response response = partyCommandService.createJoinRequest(partyId, memberId);

            // then
            assertThat(response).isNotNull();
            verify(partyJoinRequestRepository).save(any(PartyJoinRequest.class));
        }

        @Test
        @DisplayName("실패 - 이미 해당 모임의 멤버인 경우 ALREADY_MEMBER 예외가 발생한다")
        void fail_createJoinRequest_alreadyMember() {
            // given
            Long partyId = 1L;
            Long memberId = 1L;

            Party party = PartyFixture.createParty("가입 신청 모임", 10L, null);
            Member member = MemberFixture.createMember("지원자", Gender.MALE, Level.B, 1L);

            given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(memberPartyRepository.existsByPartyAndMember(party, member)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> partyCommandService.createJoinRequest(partyId, memberId))
                    .isInstanceOf(PartyException.class)
                    .satisfies(e -> assertThat(((PartyException) e).getCode()).isEqualTo(PartyErrorCode.ALREADY_MEMBER));
        }

        @Test
        @DisplayName("실패 - 대기 중인 가입 신청이 이미 존재하는 경우 JOIN_REQUEST_ALREADY_EXISTS 예외가 발생한다")
        void fail_createJoinRequest_alreadyRequested() {
            // given
            Long partyId = 1L;
            Long memberId = 1L;

            Party party = PartyFixture.createParty("가입 신청 모임", 10L, null);
            Member member = MemberFixture.createMember("지원자", Gender.MALE, Level.B, 1L);

            given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(memberPartyRepository.existsByPartyAndMember(party, member)).willReturn(false);
            given(partyJoinRequestRepository.existsByPartyAndMemberAndStatus(party, member, RequestStatus.PENDING)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> partyCommandService.createJoinRequest(partyId, memberId))
                    .isInstanceOf(PartyException.class)
                    .satisfies(e -> assertThat(((PartyException) e).getCode()).isEqualTo(PartyErrorCode.JOIN_REQUEST_ALREADY_EXISTS));
        }

        @Test
        @DisplayName("실패 - 모임 유형에 맞지 않는 성별인 경우 GENDER_NOT_MATCH 예외가 발생한다")
        void fail_createJoinRequest_genderMismatch() {
            // given
            Long partyId = 1L;
            Long memberId = 1L;

            // 여복 모임 생성
            Party party = Party.builder()
                    .partyName("여복 모임")
                    .partyType(ParticipationType.WOMEN_DOUBLES)
                    .status(PartyStatus.ACTIVE)
                    .ownerId(10L)
                    .build();
            Member member = MemberFixture.createMember("남자지원자", Gender.MALE, Level.B, 1L); // 남성 지원

            given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(memberPartyRepository.existsByPartyAndMember(party, member)).willReturn(false);
            given(partyJoinRequestRepository.existsByPartyAndMemberAndStatus(party, member, RequestStatus.PENDING)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> partyCommandService.createJoinRequest(partyId, memberId))
                    .isInstanceOf(PartyException.class)
                    .satisfies(e -> assertThat(((PartyException) e).getCode()).isEqualTo(PartyErrorCode.GENDER_NOT_MATCH));
        }

        @Test
        @DisplayName("실패 - 모임의 나이 조건에 맞지 않는 경우 AGE_NOT_MATCH 예외가 발생한다")
        void fail_createJoinRequest_ageMismatch() {
            // given
            Long partyId = 1L;
            Long memberId = 1L;

            // 1990~2000년생 모임
            Party party = Party.builder()
                    .partyName("나이 제한 모임")
                    .minBirthYear(1990)
                    .maxBirthYear(2000)
                    .status(PartyStatus.ACTIVE)
                    .ownerId(10L)
                    .build();
            // 1980년생 지원자 (범위 밖)
            Member member = MemberFixture.createMember("나이많은지원자", Gender.MALE, Level.B, 1L, LocalDate.of(1980, 1, 1));

            given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(memberPartyRepository.existsByPartyAndMember(party, member)).willReturn(false);
            given(partyJoinRequestRepository.existsByPartyAndMemberAndStatus(party, member, RequestStatus.PENDING)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> partyCommandService.createJoinRequest(partyId, memberId))
                    .isInstanceOf(PartyException.class)
                    .satisfies(e -> assertThat(((PartyException) e).getCode()).isEqualTo(PartyErrorCode.AGE_NOT_MATCH));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 파티인 경우 PARTY_NOT_FOUND 예외 발생")
        void fail_createJoinRequest_partyNotFound() {
            // given
            Member member = MemberFixture.createMember("사용자", Gender.MALE, Level.B, 1L);
            ReflectionTestUtils.setField(member, "id", 1L);
            given(memberRepository.findById(1L)).willReturn(Optional.of(member));
            given(partyRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> partyCommandService.createJoinRequest(999L, 1L))
                    .isInstanceOf(PartyException.class)
                    .satisfies(e -> assertThat(((PartyException) e).getCode()).isEqualTo(PartyErrorCode.PARTY_NOT_FOUND));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 회원인 경우 MEMBER_NOT_FOUND 예외 발생")
        void fail_createJoinRequest_memberNotFound() {
            // given
            given(memberRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> partyCommandService.createJoinRequest(1L, 1L))
                    .isInstanceOf(MemberException.class)
                    .satisfies(e -> assertThat(((MemberException) e).getCode()).isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("createParty - 모임 생성")
    class CreateParty {

        @Test
        @DisplayName("성공 - 올바른 데이터 입력 시 모임이 생성되고 채팅방이 개설된다")
        void success_createParty() {
            // given
            Long memberId = 1L;
            PartyCreateDTO.Request request = PartyCreateDTO.Request.builder()
                    .partyName("테스트 모임")
                    .partyType("혼복")
                    .minBirthYear(1990)
                    .maxBirthYear(2000)
                    .activityTime("오전")
                    .addr1("서울")
                    .addr2("강남")
                    .activityDay(List.of("월", "수"))
                    .price(10000)
                    .joinPrice(5000)
                    .designatedCock("테스트콕")
                    .maleLevel(List.of("A조"))
                    .femaleLevel(List.of("B조"))
                    .build();

            Member owner = Member.builder()
                    .id(memberId)
                    .gender(Gender.MALE)
                    .level(Level.A)
                    .birth(LocalDate.of(1995, 1, 1))
                    .build();

            PartyAddr partyAddr = PartyAddr.builder().id(1L).build();
            Party savedParty = Party.builder().id(1L).partyName("테스트 모임").build();

            given(memberRepository.findById(memberId)).willReturn(Optional.of(owner));
            given(partyAddrRepository.findByAddr1AndAddr2(anyString(), anyString())).willReturn(Optional.of(partyAddr));
            given(partyRepository.save(any(Party.class))).willReturn(savedParty);

            // when
            PartyCreateDTO.Response response = partyCommandService.createParty(memberId, request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.partyId()).isEqualTo(1L);
            verify(partyRepository, times(1)).save(any(Party.class));
            verify(chatRoomService, times(1)).createPartyChatRoom(any(Party.class), eq(owner));
        }

        @Test
        @DisplayName("실패 - 혼복 모임 생성 시 남자 급수 정보가 누락되면 MALE_LEVEL_REQUIRED 예외가 발생한다")
        void fail_createParty_mixDoubles_maleLevelMissing() {
            // given
            Long memberId = 1L;
            PartyCreateDTO.Request request = PartyCreateDTO.Request.builder()
                    .partyName("혼복 모임")
                    .partyType("혼복")
                    .minBirthYear(1990)
                    .maxBirthYear(2000)
                    .activityTime("오전")
                    .activityDay(List.of("월"))
                    .femaleLevel(List.of("A조"))
                    .maleLevel(null) // 누락
                    .build();

            Member owner = Member.builder()
                    .id(memberId)
                    .gender(Gender.MALE)
                    .birth(LocalDate.of(1995, 1, 1))
                    .build();

            given(memberRepository.findById(memberId)).willReturn(Optional.of(owner));

            // when & then
            PartyException exception = assertThrows(PartyException.class,
                    () -> partyCommandService.createParty(memberId, request));
            assertThat(exception.getCode()).isEqualTo(PartyErrorCode.MALE_LEVEL_REQUIRED);
        }

        @Test
        @DisplayName("실패 - 여복 모임 생성 시 남자 급수 정보가 포함되면 MALE_LEVEL_NOT_NEEDED 예외가 발생한다")
        void fail_createParty_womenDoubles_maleLevelProvided() {
            // given
            Long memberId = 1L;
            PartyCreateDTO.Request request = PartyCreateDTO.Request.builder()
                    .partyName("여복 모임")
                    .partyType("여복")
                    .minBirthYear(1990)
                    .maxBirthYear(2010)
                    .activityTime("오전")
                    .activityDay(List.of("토"))
                    .femaleLevel(List.of("A조"))
                    .maleLevel(List.of("A조")) // 포함됨
                    .build();

            Member owner = Member.builder()
                    .id(memberId)
                    .gender(Gender.FEMALE)
                    .birth(LocalDate.of(2000, 1, 1))
                    .build();

            given(memberRepository.findById(memberId)).willReturn(Optional.of(owner));

            // when & then
            PartyException exception = assertThrows(PartyException.class,
                    () -> partyCommandService.createParty(memberId, request));
            assertThat(exception.getCode()).isEqualTo(PartyErrorCode.MALE_LEVEL_NOT_NEEDED);
        }

        @Test
        @DisplayName("실패 - 모임 유형의 성별 조건과 생성자의 성별이 맞지 않으면 GENDER_NOT_MATCH 예외가 발생한다")
        void fail_createParty_genderMismatch() {
            // given
            Long memberId = 1L;
            PartyCreateDTO.Request request = PartyCreateDTO.Request.builder()
                    .partyName("여복 모임")
                    .partyType("여복")
                    .minBirthYear(1990)
                    .maxBirthYear(2010)
                    .activityTime("오전")
                    .activityDay(List.of("일"))
                    .femaleLevel(List.of("A조"))
                    .build();

            Member maleOwner = Member.builder()
                    .id(memberId)
                    .gender(Gender.MALE) // 남성이 여복 모임 생성 시도
                    .birth(LocalDate.of(2000, 1, 1))
                    .build();

            given(memberRepository.findById(memberId)).willReturn(Optional.of(maleOwner));

            // when & then
            PartyException exception = assertThrows(PartyException.class,
                    () -> partyCommandService.createParty(memberId, request));
            assertThat(exception.getCode()).isEqualTo(PartyErrorCode.GENDER_NOT_MATCH);
        }

        @Test
        @DisplayName("실패 - 생성자의 나이가 모임의 나이 제한 범위를 벗어나면 AGE_NOT_MATCH 예외가 발생한다")
        void fail_createParty_ageMismatch() {
            // given
            Long memberId = 1L;
            PartyCreateDTO.Request request = PartyCreateDTO.Request.builder()
                    .partyName("청년 모임")
                    .partyType("혼복")
                    .minBirthYear(2000)
                    .maxBirthYear(2010)
                    .maleLevel(List.of("A조"))
                    .femaleLevel(List.of("A조"))
                    .activityTime("오후")
                    .activityDay(List.of("금"))
                    .build();

            Member oldOwner = Member.builder()
                    .id(memberId)
                    .gender(Gender.MALE)
                    .birth(LocalDate.of(1980, 1, 1)) // 80년생이 00~10년생 모임 생성 시도
                    .build();

            given(memberRepository.findById(memberId)).willReturn(Optional.of(oldOwner));

            // when & then
            PartyException exception = assertThrows(PartyException.class,
                    () -> partyCommandService.createParty(memberId, request));
            assertThat(exception.getCode()).isEqualTo(PartyErrorCode.AGE_NOT_MATCH);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 회원인 경우 MEMBER_NOT_FOUND 예외 발생")
        void fail_createParty_memberNotFound() {
            // given
            Long memberId = 1L;
            PartyCreateDTO.Request request = PartyCreateDTO.Request.builder()
                    .partyName("테스트 모임")
                    .partyType("혼복")
                    .activityTime("오전")
                    .addr1("서울")
                    .addr2("강남")
                    .activityDay(List.of("월", "수"))
                    .price(10000)
                    .joinPrice(5000)
                    .designatedCock("테스트콕")
                    .maleLevel(List.of("A조"))
                    .femaleLevel(List.of("B조"))
                    .build();

            given(memberRepository.findById(memberId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> partyCommandService.createParty(memberId, request))
                    .isInstanceOf(MemberException.class)
                    .satisfies(e -> assertThat(((MemberException) e).getCode()).isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("updateParty")
    class UpdateParty {

        @Test
        @DisplayName("성공 - 모임장이 모임 정보를 정상적으로 수정한다")
        void success_updateParty() {
            // given
            Long partyId = 1L;
            Long memberId = 1L;

            PartyAddr addr = PartyFixture.createPartyAddr("서울", "강남");
            Member owner = MemberFixture.createMember("모임장", Gender.MALE, Level.A, 1L);
            ReflectionTestUtils.setField(owner, "id", memberId);

            Party party = PartyFixture.createParty("기존 모임명", owner.getId(), addr);
            ReflectionTestUtils.setField(party, "id", partyId);

            PartyUpdateDTO.Request request = PartyUpdateDTO.Request.builder()
                    .activityDay(List.of("토", "일"))
                    .activityTime("오전")
                    .designatedCock("새 콕")
                    .joinPrice(0)
                    .price(10000)
                    .content("새로운 내용")
                    .build();

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(memberRepository.findById(memberId)).willReturn(Optional.of(owner));

            // when
            partyCommandService.updateParty(partyId, memberId, request);

            // then
            assertThat(party.getDesignatedCock()).isEqualTo("새 콕");
            assertThat(party.getActiveDays().size()).isEqualTo(2); // 토, 일
            assertThat(party.getJoinPrice()).isEqualTo(0);
            assertThat(party.getPrice()).isEqualTo(10000);
            assertThat(party.getContent()).isEqualTo("새로운 내용");

            verify(notificationCommandService, times(1)).createNotification(any());
        }

        @Test
        @DisplayName("실패 - 조회된 모임이 없는 경우 PARTY_NOT_FOUND 예외 발생")
        void fail_updateParty_partyNotFound() {
            // given
            Long partyId = 99L;
            Long memberId = 1L;
            PartyUpdateDTO.Request request = PartyUpdateDTO.Request.builder().build();

            given(partyRepository.findById(partyId)).willReturn(Optional.empty());

            // when & then
            PartyException exception = assertThrows(PartyException.class,
                    () -> partyCommandService.updateParty(partyId, memberId, request));
            assertThat(exception.getCode()).isEqualTo(PartyErrorCode.PARTY_NOT_FOUND);
        }

        @Test
        @DisplayName("실패 - 모임장이 아닌 사용자가 수정을 시도할 경우 INSUFFICIENT_PERMISSION 예외 발생")
        void fail_updateParty_insufficientPermission() {
            // given
            Long partyId = 1L;
            Long memberId = 10L; // 일반 멤버 (ownerId=1 과 다름)

            PartyAddr addr = PartyFixture.createPartyAddr("서울", "강남");
            Member owner = MemberFixture.createMember("모임장", Gender.MALE, Level.A, 1L);
            ReflectionTestUtils.setField(owner, "id", 1L);

            Member normalMember = MemberFixture.createMember("일반멤버", Gender.MALE, Level.A, 2L);
            ReflectionTestUtils.setField(normalMember, "id", memberId);

            Party party = PartyFixture.createParty("모임명", owner.getId(), addr);
            ReflectionTestUtils.setField(party, "id", partyId);

            PartyUpdateDTO.Request request = PartyUpdateDTO.Request.builder()
                    .activityTime("오전").build();

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(memberRepository.findById(memberId)).willReturn(Optional.of(normalMember));

            // when & then
            PartyException exception = assertThrows(PartyException.class,
                    () -> partyCommandService.updateParty(partyId, memberId, request));
            assertThat(exception.getCode()).isEqualTo(PartyErrorCode.INSUFFICIENT_PERMISSION);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 회원인 경우 MEMBER_NOT_FOUND 예외 발생")
        void fail_updateParty_memberNotFound() {
            // given
            Long partyId = 1L;
            Long memberId = 1L;
            Party party = PartyFixture.createParty("모임명", 1L, null);
            PartyUpdateDTO.Request request = PartyUpdateDTO.Request.builder()
                    .activityTime("오전")
                    .build();

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(memberRepository.findById(memberId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> partyCommandService.updateParty(partyId, memberId, request))
                    .isInstanceOf(MemberException.class)
                    .satisfies(e -> assertThat(((MemberException) e).getCode()).isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("deleteParty")
    class DeleteParty {

        @Test
        @DisplayName("성공 - 모임장이 모임을 정상적으로 삭제(비활성화)한다")
        void success_deleteParty() {
            // given
            Long partyId = 1L;
            Long ownerId = 1L;

            PartyAddr addr = PartyFixture.createPartyAddr("서울", "강남");
            Member owner = MemberFixture.createMember("모임장", Gender.MALE, Level.A, ownerId);
            ReflectionTestUtils.setField(owner, "id", ownerId);

            Party party = PartyFixture.createParty("삭제할 모임", owner.getId(), addr);
            ReflectionTestUtils.setField(party, "id", partyId);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(memberRepository.findById(ownerId)).willReturn(Optional.of(owner));

            // when
            partyCommandService.deleteParty(partyId, ownerId);

            // then
            assertThat(party.getStatus()).isEqualTo(PartyStatus.INACTIVE);
            verify(applicationEventPublisher).publishEvent(any(PartyDeletedEvent.class));
        }

        @Test
        @DisplayName("실패 - 모임장이 아닌 멤버가 모임 삭제를 시도할 경우 INSUFFICIENT_PERMISSION 발생")
        void fail_deleteParty_notOwner() {
            // given
            Long partyId = 1L;
            Long ownerId = 1L;
            Long notOwnerId = 2L;

            PartyAddr addr = PartyFixture.createPartyAddr("서울", "강남");
            Member owner = MemberFixture.createMember("모임장", Gender.MALE, Level.A, ownerId);
            ReflectionTestUtils.setField(owner, "id", ownerId);

            Member notOwner = MemberFixture.createMember("일반멤버", Gender.MALE, Level.A, notOwnerId);
            ReflectionTestUtils.setField(notOwner, "id", notOwnerId);

            Party party = PartyFixture.createParty("삭제할 모임", owner.getId(), addr);
            ReflectionTestUtils.setField(party, "id", partyId);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(memberRepository.findById(notOwnerId)).willReturn(Optional.of(notOwner));

            // when & then
            PartyException exception = assertThrows(PartyException.class,
                    () -> partyCommandService.deleteParty(partyId, notOwnerId));
            assertThat(exception.getCode()).isEqualTo(PartyErrorCode.INSUFFICIENT_PERMISSION);
        }

        @Test
        @DisplayName("실패 - 이미 삭제된 모임을 삭제하려고 시도할 경우 PARTY_IS_DELETED 발생")
        void fail_deleteParty_partyDeleted() {
            // given
            Long partyId = 1L;
            Long ownerId = 1L;

            PartyAddr addr = PartyFixture.createPartyAddr("서울", "강남");
            Member owner = MemberFixture.createMember("모임장", Gender.MALE, Level.A, ownerId);
            ReflectionTestUtils.setField(owner, "id", ownerId);

            Party party = PartyFixture.createParty("이미 삭제된 모임", owner.getId(), addr);
            ReflectionTestUtils.setField(party, "id", partyId);
            party.delete(); // 상태를 INACTIVE로 변경

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(memberRepository.findById(ownerId)).willReturn(Optional.of(owner));

            // when & then
            PartyException exception = assertThrows(PartyException.class,
                    () -> partyCommandService.deleteParty(partyId, ownerId));
            assertThat(exception.getCode()).isEqualTo(PartyErrorCode.PARTY_IS_DELETED);
        }

        @Test
        @DisplayName("실패 - 조회된 모임이 존재하지 않을 경우 PARTY_NOT_FOUND 발생")
        void fail_deleteParty_partyNotFound() {
            // given
            Long invalidId = 999L;
            given(partyRepository.findById(invalidId)).willReturn(Optional.empty());

            // when & then
            PartyException exception = assertThrows(PartyException.class,
                    () -> partyCommandService.deleteParty(invalidId, 1L));
            assertThat(exception.getCode()).isEqualTo(PartyErrorCode.PARTY_NOT_FOUND);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 회원인 경우 MEMBER_NOT_FOUND 예외 발생")
        void fail_deleteParty_memberNotFound() {
            // given
            Long partyId = 1L;
            Long memberId = 1L;
            Party party = PartyFixture.createParty("모임명", 1L, null);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(memberRepository.findById(memberId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> partyCommandService.deleteParty(partyId, memberId))
                    .isInstanceOf(MemberException.class)
                    .satisfies(e -> assertThat(((MemberException) e).getCode()).isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("updateMemberRole")
    class UpdateMemberRole {

        @Test
        @DisplayName("성공 - 모임장이 일반 멤버를 부모임장으로 지정하면 기존 부모임장은 일반 멤버로 강등되고 새 부모임장이 지정된다")
        void success_updateMemberRole() {
            // given
            Long partyId = 1L;
            Long currentOwnerId = 1L;
            Long targetMemberId = 10L;
            Long oldSubManagerId = 20L;

            PartyAddr addr = PartyFixture.createPartyAddr("서울", "강남");
            Member owner = MemberFixture.createMember("모임장", Gender.MALE, Level.A, currentOwnerId);
            ReflectionTestUtils.setField(owner, "id", currentOwnerId);

            Member targetMember = MemberFixture.createMember("일반멤버", Gender.MALE, Level.A, targetMemberId);
            ReflectionTestUtils.setField(targetMember, "id", targetMemberId);

            Member oldSubManager = MemberFixture.createMember("기존부모임장", Gender.MALE, Level.A, oldSubManagerId);
            ReflectionTestUtils.setField(oldSubManager, "id", oldSubManagerId);

            Party party = PartyFixture.createParty("모임명", owner.getId(), addr);
            ReflectionTestUtils.setField(party, "id", partyId);

            MemberParty targetMemberParty = MemberFixture.createMemberParty(party, targetMember, Role.PARTY_MEMBER);
            MemberParty oldSubManagerParty = MemberFixture.createMemberParty(party, oldSubManager, Role.PARTY_SUBMANAGER);

            PartyMemberRoleDTO.Request request = new PartyMemberRoleDTO.Request(Role.PARTY_SUBMANAGER);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(memberRepository.findById(targetMemberId)).willReturn(Optional.of(targetMember));
            given(memberPartyRepository.findByPartyAndMember(party, targetMember)).willReturn(Optional.of(targetMemberParty));
            given(memberPartyRepository.findByPartyIdAndRole(partyId, Role.PARTY_SUBMANAGER)).willReturn(Optional.of(oldSubManagerParty));
            given(memberPartyRepository.findAllByPartyIdWithMember(partyId)).willReturn(List.of(targetMemberParty, oldSubManagerParty));

            // when
            partyCommandService.updateMemberRole(partyId, targetMemberId, currentOwnerId, request);

            // then
            assertThat(targetMemberParty.getRole()).isEqualTo(Role.PARTY_SUBMANAGER);
            assertThat(oldSubManagerParty.getRole()).isEqualTo(Role.PARTY_MEMBER);
            verify(notificationCommandService, times(4)).createNotification(any());
        }

        @Test
        @DisplayName("실패 - 이미 요청한 역할과 같은 역할인 경우 변경 없이 반환된다")
        void fail_updateMemberRole_sameRole() {
            // given
            Long partyId = 1L;
            Long ownerId = 1L;
            Long targetId = 10L;

            PartyAddr addr = PartyFixture.createPartyAddr("서울", "강남");
            Member owner = MemberFixture.createMember("모임장", Gender.MALE, Level.A, ownerId);
            ReflectionTestUtils.setField(owner, "id", ownerId);

            Member targetMember = MemberFixture.createMember("타겟", Gender.MALE, Level.A, targetId);
            ReflectionTestUtils.setField(targetMember, "id", targetId);

            Party party = PartyFixture.createParty("모임명", owner.getId(), addr);
            ReflectionTestUtils.setField(party, "id", partyId);

            MemberParty targetMemberParty = spy(MemberFixture.createMemberParty(party, targetMember, Role.PARTY_SUBMANAGER));
            PartyMemberRoleDTO.Request request = new PartyMemberRoleDTO.Request(Role.PARTY_SUBMANAGER);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(memberRepository.findById(targetId)).willReturn(Optional.of(targetMember));
            given(memberPartyRepository.findByPartyAndMember(party, targetMember)).willReturn(Optional.of(targetMemberParty));

            // when
            partyCommandService.updateMemberRole(partyId, targetId, ownerId, request);

            // then
            verify(targetMemberParty, never()).changeRole(any());
            verify(notificationCommandService, never()).createNotification(any());
        }

        @Test
        @DisplayName("실패 - 대상 멤버가 이미 모임장인 경우 권한을 변경하려 하면 CANNOT_ASSIGN_TO_OWNER 발생")
        void fail_updateMemberRole_targetIsOwner() {
            // given
            Long partyId = 1L;
            Long ownerId = 1L;

            PartyAddr addr = PartyFixture.createPartyAddr("서울", "강남");
            Member owner = MemberFixture.createMember("모임장", Gender.MALE, Level.A, ownerId);
            ReflectionTestUtils.setField(owner, "id", ownerId);

            Party party = PartyFixture.createParty("모임명", owner.getId(), addr);
            ReflectionTestUtils.setField(party, "id", partyId);

            // 타겟이 이미 모임장 권한을 가짐
            MemberParty memberParty = MemberFixture.createMemberParty(party, owner, Role.PARTY_MANAGER);
            PartyMemberRoleDTO.Request request = new PartyMemberRoleDTO.Request(Role.PARTY_SUBMANAGER);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(memberRepository.findById(ownerId)).willReturn(Optional.of(owner));
            given(memberPartyRepository.findByPartyAndMember(party, owner)).willReturn(Optional.of(memberParty));

            // when & then
            PartyException exception = assertThrows(PartyException.class,
                    () -> partyCommandService.updateMemberRole(partyId, ownerId, ownerId, request));
            assertThat(exception.getCode()).isEqualTo(PartyErrorCode.CANNOT_ASSIGN_TO_OWNER);
        }

        @Test
        @DisplayName("실패 - 현재 사용자가 모임장이 아닐 경우 INSUFFICIENT_PERMISSION 발생")
        void fail_updateMemberRole_notOwner() {
            // given
            Long partyId = 1L;
            Long ownerId = 1L;
            Long notOwnerId = 2L;
            Long targetId = 10L;

            PartyAddr addr = PartyFixture.createPartyAddr("서울", "강남");
            Member owner = MemberFixture.createMember("모임장", Gender.MALE, Level.A, ownerId);
            ReflectionTestUtils.setField(owner, "id", ownerId);

            Member notOwner = MemberFixture.createMember("일반멤버", Gender.MALE, Level.A, notOwnerId);
            ReflectionTestUtils.setField(notOwner, "id", notOwnerId);

            Member targetMember = MemberFixture.createMember("타겟", Gender.MALE, Level.A, targetId);
            ReflectionTestUtils.setField(targetMember, "id", targetId);

            Party party = PartyFixture.createParty("모임명", owner.getId(), addr);
            ReflectionTestUtils.setField(party, "id", partyId);

            MemberParty targetMemberParty = MemberFixture.createMemberParty(party, targetMember, Role.PARTY_MEMBER);
            PartyMemberRoleDTO.Request request = new PartyMemberRoleDTO.Request(Role.PARTY_SUBMANAGER);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(memberRepository.findById(targetId)).willReturn(Optional.of(targetMember));
            given(memberPartyRepository.findByPartyAndMember(party, targetMember)).willReturn(Optional.of(targetMemberParty));

            // when & then (notOwnerId를 currentMemberId로 전달하여 실행)
            PartyException exception = assertThrows(PartyException.class,
                    () -> partyCommandService.updateMemberRole(partyId, targetId, notOwnerId, request));
            assertThat(exception.getCode()).isEqualTo(PartyErrorCode.INSUFFICIENT_PERMISSION);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 파티인 경우 PARTY_NOT_FOUND 예외 발생")
        void fail_updateMemberRole_partyNotFound() {
            // given
            PartyMemberRoleDTO.Request request = new PartyMemberRoleDTO.Request(Role.PARTY_SUBMANAGER);
            given(partyRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> partyCommandService.updateMemberRole(999L, 1L, 1L, request))
                    .isInstanceOf(PartyException.class)
                    .satisfies(e -> assertThat(((PartyException) e).getCode()).isEqualTo(PartyErrorCode.PARTY_NOT_FOUND));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 회원인 경우 MEMBER_NOT_FOUND 예외 발생")
        void fail_updateMemberRole_memberNotFound() {
            // given
            Long partyId = 1L;
            Party party = PartyFixture.createParty("모임명", 1L, null);
            PartyMemberRoleDTO.Request request = new PartyMemberRoleDTO.Request(Role.PARTY_SUBMANAGER);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(memberRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> partyCommandService.updateMemberRole(partyId, 1L, 1L, request))
                    .isInstanceOf(MemberException.class)
                    .satisfies(e -> assertThat(((MemberException) e).getCode()).isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("removeMember")
    class RemoveMember {

        @Test
        @DisplayName("성공 - 모임장이 일반 멤버를 성공적으로 강퇴한다")
        void success_removeMember() {
            // given
            Long partyId = 1L;
            Long ownerId = 1L;
            Long targetMemberId = 10L;

            PartyAddr addr = PartyFixture.createPartyAddr("서울", "강남");
            Member owner = MemberFixture.createMember("모임장", Gender.MALE, Level.A, ownerId);
            ReflectionTestUtils.setField(owner, "id", ownerId);
            Member targetMember = MemberFixture.createMember("타겟", Gender.MALE, Level.A, targetMemberId);
            ReflectionTestUtils.setField(targetMember, "id", targetMemberId);

            Party party = PartyFixture.createParty("모임명", owner.getId(), addr);
            ReflectionTestUtils.setField(party, "id", partyId);

            MemberParty ownerParty = MemberFixture.createMemberParty(party, owner, Role.PARTY_MANAGER);
            MemberParty targetMemberParty = MemberFixture.createMemberParty(party, targetMember, Role.PARTY_MEMBER);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(memberRepository.findById(ownerId)).willReturn(Optional.of(owner));
            given(memberRepository.findById(targetMemberId)).willReturn(Optional.of(targetMember));
            given(memberPartyRepository.findByPartyAndMember(party, owner)).willReturn(Optional.of(ownerParty));
            given(memberPartyRepository.findByPartyAndMember(party, targetMember)).willReturn(Optional.of(targetMemberParty));

            // when
            partyCommandService.removeMember(partyId, targetMemberId, ownerId);

            // then
            verify(memberPartyRepository, times(1)).delete(targetMemberParty);
            verify(chatRoomService, times(1)).leavePartyChatRoom(partyId, targetMemberId);
        }

        @Test
        @DisplayName("성공 - 부모임장이 일반 멤버를 성공적으로 강퇴한다")
        void success_removeMember_bySubManager() {
            // given
            Long partyId = 1L;
            Long subManagerId = 2L;
            Long targetMemberId = 10L;

            PartyAddr addr = PartyFixture.createPartyAddr("서울", "강남");
            Member subManager = MemberFixture.createMember("부모임장", Gender.MALE, Level.A, subManagerId);
            ReflectionTestUtils.setField(subManager, "id", subManagerId);
            Member targetMember = MemberFixture.createMember("일반멤버", Gender.MALE, Level.A, targetMemberId);
            ReflectionTestUtils.setField(targetMember, "id", targetMemberId);

            Party party = PartyFixture.createParty("모임명", 1L, addr); // ownerId = 1L
            ReflectionTestUtils.setField(party, "id", partyId);

            MemberParty subManagerParty = MemberFixture.createMemberParty(party, subManager, Role.PARTY_SUBMANAGER);
            MemberParty targetMemberParty = MemberFixture.createMemberParty(party, targetMember, Role.PARTY_MEMBER);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(memberRepository.findById(subManagerId)).willReturn(Optional.of(subManager));
            given(memberRepository.findById(targetMemberId)).willReturn(Optional.of(targetMember));
            given(memberPartyRepository.findByPartyAndMember(party, subManager)).willReturn(Optional.of(subManagerParty));
            given(memberPartyRepository.findByPartyAndMember(party, targetMember)).willReturn(Optional.of(targetMemberParty));

            // when
            partyCommandService.removeMember(partyId, targetMemberId, subManagerId);

            // then
            verify(memberPartyRepository, times(1)).delete(targetMemberParty);
            verify(chatRoomService, times(1)).leavePartyChatRoom(partyId, targetMemberId);
        }

        @Test
        @DisplayName("실패 - 권한이 없는 멤버가 타인을 강퇴하려 하면 INSUFFICIENT_PERMISSION 발생")
        void fail_removeMember_insufficientPermission() {
            // given
            Long partyId = 1L;
            Long subManagerId = 2L;
            Long targetOwnerId = 1L;

            PartyAddr addr = PartyFixture.createPartyAddr("서울", "강남");
            Member owner = MemberFixture.createMember("모임장", Gender.MALE, Level.A, targetOwnerId);
            ReflectionTestUtils.setField(owner, "id", targetOwnerId);
            Member subManager = MemberFixture.createMember("부모임장", Gender.MALE, Level.A, subManagerId);
            ReflectionTestUtils.setField(subManager, "id", subManagerId);

            Party party = PartyFixture.createParty("모임명", owner.getId(), addr);
            ReflectionTestUtils.setField(party, "id", partyId);

            MemberParty ownerParty = MemberFixture.createMemberParty(party, owner, Role.PARTY_MANAGER);
            MemberParty subManagerParty = MemberFixture.createMemberParty(party, subManager, Role.PARTY_SUBMANAGER);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(memberRepository.findById(subManagerId)).willReturn(Optional.of(subManager));
            given(memberRepository.findById(targetOwnerId)).willReturn(Optional.of(owner));
            given(memberPartyRepository.findByPartyAndMember(party, subManager)).willReturn(Optional.of(subManagerParty));
            given(memberPartyRepository.findByPartyAndMember(party, owner)).willReturn(Optional.of(ownerParty));

            // when & then
            PartyException exception = assertThrows(PartyException.class,
                    () -> partyCommandService.removeMember(partyId, targetOwnerId, subManagerId));
            assertThat(exception.getCode()).isEqualTo(PartyErrorCode.INSUFFICIENT_PERMISSION);
        }

        @Test
        @DisplayName("실패 - 모임장이 자신을 강퇴하려 할 경우 CANNOT_REMOVE_SELF 발생")
        void fail_removeMember_cannotRemoveSelf() {
            // given
            Long partyId = 1L;
            Long ownerId = 1L;

            PartyAddr addr = PartyFixture.createPartyAddr("서울", "강남");
            Member owner = MemberFixture.createMember("모임장", Gender.MALE, Level.A, ownerId);
            ReflectionTestUtils.setField(owner, "id", ownerId);

            Party party = PartyFixture.createParty("모임명", owner.getId(), addr);
            ReflectionTestUtils.setField(party, "id", partyId);

            MemberParty ownerParty = MemberFixture.createMemberParty(party, owner, Role.PARTY_MANAGER);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(memberRepository.findById(ownerId)).willReturn(Optional.of(owner));
            given(memberPartyRepository.findByPartyAndMember(party, owner)).willReturn(Optional.of(ownerParty));

            // when & then
            PartyException exception = assertThrows(PartyException.class,
                    () -> partyCommandService.removeMember(partyId, ownerId, ownerId));
            assertThat(exception.getCode()).isEqualTo(PartyErrorCode.CANNOT_REMOVE_SELF);
        }

        @Test
        @DisplayName("실패 - 대상 멤버가 모임 소속이 아닐 경우 NOT_MEMBER 발생")
        void fail_removeMember_notMember() {
            // given
            Long partyId = 1L;
            Long ownerId = 1L;
            Long targetMemberId = 10L;

            PartyAddr addr = PartyFixture.createPartyAddr("서울", "강남");
            Member owner = MemberFixture.createMember("모임장", Gender.MALE, Level.A, ownerId);
            ReflectionTestUtils.setField(owner, "id", ownerId);
            Member targetMember = MemberFixture.createMember("타겟", Gender.MALE, Level.A, targetMemberId);
            ReflectionTestUtils.setField(targetMember, "id", targetMemberId);

            Party party = PartyFixture.createParty("모임명", owner.getId(), addr);
            ReflectionTestUtils.setField(party, "id", partyId);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(memberRepository.findById(ownerId)).willReturn(Optional.of(owner));
            given(memberRepository.findById(targetMemberId)).willReturn(Optional.of(targetMember));

            // 타겟 멤버가 모임 소속이 아님 -> findMemberPartyOrThrow 에서 NOT_MEMBER 발생
            given(memberPartyRepository.findByPartyAndMember(party, targetMember)).willReturn(Optional.empty());

            // when & then
            PartyException exception = assertThrows(PartyException.class,
                    () -> partyCommandService.removeMember(partyId, targetMemberId, ownerId));
            assertThat(exception.getCode()).isEqualTo(PartyErrorCode.NOT_MEMBER);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 파티인 경우 PARTY_NOT_FOUND 예외 발생")
        void fail_removeMember_partyNotFound() {
            // given
            given(partyRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> partyCommandService.removeMember(999L, 1L, 1L))
                    .isInstanceOf(PartyException.class)
                    .satisfies(e -> assertThat(((PartyException) e).getCode()).isEqualTo(PartyErrorCode.PARTY_NOT_FOUND));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 회원인 경우 MEMBER_NOT_FOUND 예외 발생")
        void fail_removeMember_memberNotFound() {
            // given
            Long partyId = 1L;
            Party party = PartyFixture.createParty("모임명", 1L, null);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(memberRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> partyCommandService.removeMember(partyId, 10L, 1L))
                    .isInstanceOf(MemberException.class)
                    .satisfies(e -> assertThat(((MemberException) e).getCode()).isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("actionJoinRequest")
    class ActionJoinRequest {

        @Test
        @DisplayName("성공 - 모임장이 가입 신청을 승인하고 알림과 채팅방 진입, 이벤트를 발생시킨다")
        void success_actionJoinRequest_approve() {
            // given
            Long partyId = 1L;
            Long ownerId = 10L;
            Long requestId = 100L;

            PartyAddr addr = PartyFixture.createPartyAddr("서울", "강남");
            Member owner = MemberFixture.createMember("모임장", Gender.MALE, Level.A, ownerId);
            ReflectionTestUtils.setField(owner, "id", ownerId);
            Party party = PartyFixture.createParty("모임명", owner.getId(), addr);
            ReflectionTestUtils.setField(party, "id", partyId);

            Member applicant = MemberFixture.createMember("지원자", Gender.FEMALE, Level.B, 20L);
            ReflectionTestUtils.setField(applicant, "id", 20L);

            PartyJoinRequest joinRequest = PartyJoinRequest.builder()
                    .party(party)
                    .member(applicant)
                    .status(RequestStatus.PENDING)
                    .build();
            ReflectionTestUtils.setField(joinRequest, "id", requestId);

            PartyJoinActionDTO.Request requestDTO = new PartyJoinActionDTO.Request(RequestAction.APPROVE);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(partyJoinRequestRepository.findById(requestId)).willReturn(Optional.of(joinRequest));
            given(memberPartyRepository.existsByPartyAndMember(party, applicant)).willReturn(false);

            // when
            partyCommandService.actionJoinRequest(partyId, ownerId, requestDTO, requestId);

            // then
            assertThat(joinRequest.getStatus()).isEqualTo(RequestStatus.APPROVED);
            verify(chatRoomService).joinPartyChatRoom(partyId, applicant);
            verify(applicationEventPublisher).publishEvent(any(PartyMemberJoinedEvent.class));
            verify(notificationCommandService).createNotification(any());
        }

        @Test
        @DisplayName("성공 - 모임장이 가입 신청을 거절하면 상태만 REJECTED로 바뀌고 다른 사이드이펙트가 발생하지 않는다")
        void success_actionJoinRequest_reject() {
            // given
            Long partyId = 1L;
            Long ownerId = 10L;
            Long requestId = 100L;

            PartyAddr addr = PartyFixture.createPartyAddr("서울", "강남");
            Member owner = MemberFixture.createMember("모임장", Gender.MALE, Level.A, ownerId);
            ReflectionTestUtils.setField(owner, "id", ownerId);
            Party party = PartyFixture.createParty("모임명", owner.getId(), addr);
            ReflectionTestUtils.setField(party, "id", partyId);

            Member applicant = MemberFixture.createMember("지원자", Gender.FEMALE, Level.B, 20L);
            ReflectionTestUtils.setField(applicant, "id", 20L);

            PartyJoinRequest joinRequest = PartyJoinRequest.builder()
                    .party(party)
                    .member(applicant)
                    .status(RequestStatus.PENDING)
                    .build();
            ReflectionTestUtils.setField(joinRequest, "id", requestId);

            PartyJoinActionDTO.Request requestDTO = new PartyJoinActionDTO.Request(RequestAction.REJECT);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(partyJoinRequestRepository.findById(requestId)).willReturn(Optional.of(joinRequest));
            given(memberPartyRepository.existsByPartyAndMember(party, applicant)).willReturn(false);

            // when
            partyCommandService.actionJoinRequest(partyId, ownerId, requestDTO, requestId);

            // then
            assertThat(joinRequest.getStatus()).isEqualTo(RequestStatus.REJECTED);
            verifyNoInteractions(chatRoomService);
            verifyNoInteractions(applicationEventPublisher);
            verifyNoInteractions(notificationCommandService);
        }

        @Test
        @DisplayName("실패 - 대상자가 이미 모임 멤버인 경우 ALREADY_MEMBER 검증 에러가 발생한다")
        void fail_actionJoinRequest_alreadyMember() {
            // given
            Long partyId = 1L;
            Long ownerId = 10L;
            Long requestId = 100L;

            PartyAddr addr = PartyFixture.createPartyAddr("서울", "강남");
            Member owner = MemberFixture.createMember("모임장", Gender.MALE, Level.A, ownerId);
            ReflectionTestUtils.setField(owner, "id", ownerId);
            Party party = PartyFixture.createParty("모임명", owner.getId(), addr);
            ReflectionTestUtils.setField(party, "id", partyId);

            Member applicant = MemberFixture.createMember("지원자", Gender.FEMALE, Level.B, 20L);
            ReflectionTestUtils.setField(applicant, "id", 20L);

            PartyJoinRequest joinRequest = PartyJoinRequest.builder()
                    .party(party)
                    .member(applicant)
                    .status(RequestStatus.PENDING)
                    .build();
            ReflectionTestUtils.setField(joinRequest, "id", requestId);

            PartyJoinActionDTO.Request requestDTO = new PartyJoinActionDTO.Request(RequestAction.APPROVE);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(partyJoinRequestRepository.findById(requestId)).willReturn(Optional.of(joinRequest));
            given(memberPartyRepository.existsByPartyAndMember(party, applicant)).willReturn(true); // 이미 멤버

            // when & then
            PartyException exception = assertThrows(PartyException.class,
                    () -> partyCommandService.actionJoinRequest(partyId, ownerId, requestDTO, requestId));
            assertThat(exception.getCode()).isEqualTo(PartyErrorCode.ALREADY_MEMBER);
        }

        @Test
        @DisplayName("실패 - 이미 처리된 가입 신청을 다시 처리하려 할 때 JOIN_REQUEST_ALREADY_ACTIONS 발생")
        void fail_actionJoinRequest_alreadyActions() {
            // given
            Long partyId = 1L;
            Long ownerId = 10L;
            Long requestId = 100L;

            PartyAddr addr = PartyFixture.createPartyAddr("서울", "강남");
            Member owner = MemberFixture.createMember("모임장", Gender.MALE, Level.A, ownerId);
            ReflectionTestUtils.setField(owner, "id", ownerId);
            Party party = PartyFixture.createParty("모임명", owner.getId(), addr);
            ReflectionTestUtils.setField(party, "id", partyId);

            Member applicant = MemberFixture.createMember("지원자", Gender.FEMALE, Level.B, 20L);
            ReflectionTestUtils.setField(applicant, "id", 20L);

            PartyJoinRequest joinRequest = PartyJoinRequest.builder()
                    .party(party)
                    .member(applicant)
                    .status(RequestStatus.APPROVED) // 이미 승인됨
                    .build();
            ReflectionTestUtils.setField(joinRequest, "id", requestId);

            PartyJoinActionDTO.Request requestDTO = new PartyJoinActionDTO.Request(RequestAction.REJECT);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(partyJoinRequestRepository.findById(requestId)).willReturn(Optional.of(joinRequest));
            given(memberPartyRepository.existsByPartyAndMember(party, applicant)).willReturn(false);

            // when & then
            PartyException exception = assertThrows(PartyException.class,
                    () -> partyCommandService.actionJoinRequest(partyId, ownerId, requestDTO, requestId));
            assertThat(exception.getCode()).isEqualTo(PartyErrorCode.JOIN_REQUEST_ALREADY_ACTIONS);
        }

        @Test
        @DisplayName("실패 - 해당 가입 요청을 찾을 수 없는 경우 JOIN_REQUEST_NOT_FOUND 발생")
        void fail_actionJoinRequest_notFound() {
            // given
            Long partyId = 1L;
            Long ownerId = 10L;
            Long requestId = 999L; // 존재하지 않는 ID

            PartyAddr addr = PartyFixture.createPartyAddr("서울", "강남");
            Member owner = MemberFixture.createMember("모임장", Gender.MALE, Level.A, ownerId);
            ReflectionTestUtils.setField(owner, "id", ownerId);
            Party party = PartyFixture.createParty("모임명", owner.getId(), addr);
            ReflectionTestUtils.setField(party, "id", partyId);

            PartyJoinActionDTO.Request requestDTO = new PartyJoinActionDTO.Request(RequestAction.APPROVE);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(partyJoinRequestRepository.findById(requestId)).willReturn(Optional.empty());

            // when & then
            PartyException exception = assertThrows(PartyException.class,
                    () -> partyCommandService.actionJoinRequest(partyId, ownerId, requestDTO, requestId));
            assertThat(exception.getCode()).isEqualTo(PartyErrorCode.JOIN_REQUEST_NOT_FOUND);
        }

        @Test
        @DisplayName("실패 - 모임장이 아닌 사용자가 가입 신청을 처리하려 할 때 INSUFFICIENT_PERMISSION 발생")
        void fail_actionJoinRequest_insufficientPermission() {
            // given
            Long partyId = 1L;
            Long ownerId = 10L;
            Long notOwnerId = 99L;
            Long requestId = 100L;

            PartyAddr addr = PartyFixture.createPartyAddr("서울", "강남");
            Member owner = MemberFixture.createMember("모임장", Gender.MALE, Level.A, ownerId); // 실제 모임장
            ReflectionTestUtils.setField(owner, "id", ownerId);
            Party party = PartyFixture.createParty("모임명", owner.getId(), addr);
            ReflectionTestUtils.setField(party, "id", partyId);

            Member applicant = MemberFixture.createMember("지원자", Gender.FEMALE, Level.B, 20L);
            ReflectionTestUtils.setField(applicant, "id", 20L);

            PartyJoinRequest joinRequest = PartyJoinRequest.builder()
                    .party(party)
                    .member(applicant)
                    .status(RequestStatus.PENDING)
                    .build();
            ReflectionTestUtils.setField(joinRequest, "id", requestId);

            PartyJoinActionDTO.Request requestDTO = new PartyJoinActionDTO.Request(RequestAction.APPROVE);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(partyJoinRequestRepository.findById(requestId)).willReturn(Optional.of(joinRequest));

            // when & then
            PartyException exception = assertThrows(PartyException.class,
                    () -> partyCommandService.actionJoinRequest(partyId, notOwnerId, requestDTO, requestId));
            assertThat(exception.getCode()).isEqualTo(PartyErrorCode.INSUFFICIENT_PERMISSION);
        }

        @Test
        @DisplayName("실패 - 처리하려는 가입 신청이 해당 모임의 것이 아닌 경우 JOIN_REQUEST_PARTY_NOT_FOUND 발생")
        void fail_actionJoinRequest_joinRequestPartyNotFound() {
            // given
            Long partyId = 1L;
            Long wrongPartyId = 2L;
            Long ownerId = 10L;
            Long requestId = 100L;

            PartyAddr addr = PartyFixture.createPartyAddr("서울", "강남");
            Member owner = MemberFixture.createMember("모임장", Gender.MALE, Level.A, ownerId);
            ReflectionTestUtils.setField(owner, "id", ownerId);

            Party targetParty = PartyFixture.createParty("대상 모임", owner.getId(), addr);
            ReflectionTestUtils.setField(targetParty, "id", partyId);

            Party wrongParty = PartyFixture.createParty("다른 모임", owner.getId(), addr);
            ReflectionTestUtils.setField(wrongParty, "id", wrongPartyId);

            Member applicant = MemberFixture.createMember("지원자", Gender.FEMALE, Level.B, 20L);
            ReflectionTestUtils.setField(applicant, "id", 20L);

            // 다른 모임으로 가입신청
            PartyJoinRequest joinRequest = PartyJoinRequest.builder()
                    .party(wrongParty)
                    .member(applicant)
                    .status(RequestStatus.PENDING)
                    .build();
            ReflectionTestUtils.setField(joinRequest, "id", requestId);

            PartyJoinActionDTO.Request requestDTO = new PartyJoinActionDTO.Request(RequestAction.APPROVE);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(targetParty));
            given(partyJoinRequestRepository.findById(requestId)).willReturn(Optional.of(joinRequest));

            // when & then
            PartyException exception = assertThrows(PartyException.class,
                    () -> partyCommandService.actionJoinRequest(partyId, ownerId, requestDTO, requestId));
            assertThat(exception.getCode()).isEqualTo(PartyErrorCode.JOIN_REQUEST_PARTY_NOT_FOUND);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 파티인 경우 PARTY_NOT_FOUND 예외 발생")
        void fail_actionJoinRequest_partyNotFound() {
            // given
            given(partyRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> partyCommandService.actionJoinRequest(999L, 1L, new PartyJoinActionDTO.Request(RequestAction.APPROVE), 1L))
                    .isInstanceOf(PartyException.class)
                    .satisfies(e -> assertThat(((PartyException) e).getCode()).isEqualTo(PartyErrorCode.PARTY_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("createInvitation")
    class CreateInvitation {

        @Test
        @DisplayName("성공 - 모임장이 새로운 멤버를 초대하고 invitationId를 반환한다")
        void success_createInvitation() {
            // given
            Long partyId = 1L;
            Long ownerId = 10L;
            Long inviteeId = 20L;

            PartyAddr addr = PartyFixture.createPartyAddr("서울", "강남");
            Member owner = MemberFixture.createMember("모임장", Gender.MALE, Level.A, ownerId);
            ReflectionTestUtils.setField(owner, "id", ownerId);
            Member invitee = MemberFixture.createMember("초대대상", Gender.FEMALE, Level.B, inviteeId);
            ReflectionTestUtils.setField(invitee, "id", inviteeId);
            Party party = PartyFixture.createParty("모임명", ownerId, addr);
            ReflectionTestUtils.setField(party, "id", partyId);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(memberRepository.findById(ownerId)).willReturn(Optional.of(owner));
            given(memberRepository.findById(inviteeId)).willReturn(Optional.of(invitee));
            given(memberPartyRepository.existsByPartyAndMember(party, invitee)).willReturn(false);
            given(partyInvitationRepository.existsByPartyAndInviteeAndStatus(party, invitee, RequestStatus.PENDING)).willReturn(false);

            PartyInvitation savedInvitation = PartyInvitation.create(party, owner, invitee);
            ReflectionTestUtils.setField(savedInvitation, "id", 100L);
            given(partyInvitationRepository.save(any())).willReturn(savedInvitation);

            // when
            PartyInviteCreateDTO.Response response = partyCommandService.createInvitation(partyId, inviteeId, ownerId);

            // then
            assertThat(response.invitationId()).isEqualTo(100L);
            verify(notificationCommandService).createNotification(any());
        }

        @Test
        @DisplayName("실패 - 모임장이 아닌 사용자가 초대하려 하면 INSUFFICIENT_PERMISSION 발생")
        void fail_createInvitation_notOwner() {
            // given
            Long partyId = 1L;
            Long nonOwnerId = 99L;
            Long inviteeId = 20L;

            PartyAddr addr = PartyFixture.createPartyAddr("서울", "강남");
            Member nonOwner = MemberFixture.createMember("일반멤버", Gender.MALE, Level.B, nonOwnerId);
            ReflectionTestUtils.setField(nonOwner, "id", nonOwnerId);
            Member invitee = MemberFixture.createMember("초대대상", Gender.FEMALE, Level.B, inviteeId);
            ReflectionTestUtils.setField(invitee, "id", inviteeId);
            Party party = PartyFixture.createParty("모임명", 10L, addr); // ownerId = 10L
            ReflectionTestUtils.setField(party, "id", partyId);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(memberRepository.findById(nonOwnerId)).willReturn(Optional.of(nonOwner));
            given(memberRepository.findById(inviteeId)).willReturn(Optional.of(invitee));

            // when & then
            PartyException exception = assertThrows(PartyException.class,
                    () -> partyCommandService.createInvitation(partyId, inviteeId, nonOwnerId));
            assertThat(exception.getCode()).isEqualTo(PartyErrorCode.INSUFFICIENT_PERMISSION);
        }

        @Test
        @DisplayName("실패 - 이미 모임에 가입한 멤버를 초대하면 ALREADY_MEMBER 발생")
        void fail_createInvitation_alreadyMember() {
            // given
            Long partyId = 1L;
            Long ownerId = 10L;
            Long inviteeId = 20L;

            PartyAddr addr = PartyFixture.createPartyAddr("서울", "강남");
            Member owner = MemberFixture.createMember("모임장", Gender.MALE, Level.A, ownerId);
            ReflectionTestUtils.setField(owner, "id", ownerId);
            Member invitee = MemberFixture.createMember("대상멤버", Gender.FEMALE, Level.B, inviteeId);
            ReflectionTestUtils.setField(invitee, "id", inviteeId);
            Party party = PartyFixture.createParty("모임명", ownerId, addr);
            ReflectionTestUtils.setField(party, "id", partyId);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(memberRepository.findById(ownerId)).willReturn(Optional.of(owner));
            given(memberRepository.findById(inviteeId)).willReturn(Optional.of(invitee));
            given(memberPartyRepository.existsByPartyAndMember(party, invitee)).willReturn(true); // 이미 멤버

            // when & then
            PartyException exception = assertThrows(PartyException.class,
                    () -> partyCommandService.createInvitation(partyId, inviteeId, ownerId));
            assertThat(exception.getCode()).isEqualTo(PartyErrorCode.ALREADY_MEMBER);
        }

        @Test
        @DisplayName("실패 - 이미 대기 중인 초대가 있는 멤버를 중복 초대하면 INVITATION_ALREADY_EXISTS 발생")
        void fail_createInvitation_duplicateInvitation() {
            // given
            Long partyId = 1L;
            Long ownerId = 10L;
            Long inviteeId = 20L;

            PartyAddr addr = PartyFixture.createPartyAddr("서울", "강남");
            Member owner = MemberFixture.createMember("모임장", Gender.MALE, Level.A, ownerId);
            ReflectionTestUtils.setField(owner, "id", ownerId);
            Member invitee = MemberFixture.createMember("대상멤버", Gender.FEMALE, Level.B, inviteeId);
            ReflectionTestUtils.setField(invitee, "id", inviteeId);
            Party party = PartyFixture.createParty("모임명", ownerId, addr);
            ReflectionTestUtils.setField(party, "id", partyId);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(memberRepository.findById(ownerId)).willReturn(Optional.of(owner));
            given(memberRepository.findById(inviteeId)).willReturn(Optional.of(invitee));
            given(memberPartyRepository.existsByPartyAndMember(party, invitee)).willReturn(false);
            given(partyInvitationRepository.existsByPartyAndInviteeAndStatus(party, invitee, RequestStatus.PENDING)).willReturn(true); // 이미 대기중 초대

            // when & then
            PartyException exception = assertThrows(PartyException.class,
                    () -> partyCommandService.createInvitation(partyId, inviteeId, ownerId));
            assertThat(exception.getCode()).isEqualTo(PartyErrorCode.INVITATION_ALREADY_EXISTS);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 파티인 경우 PARTY_NOT_FOUND 예외 발생")
        void fail_createInvitation_partyNotFound() {
            // given
            given(partyRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> partyCommandService.createInvitation(999L, 1L, 1L))
                    .isInstanceOf(PartyException.class)
                    .satisfies(e -> assertThat(((PartyException) e).getCode()).isEqualTo(PartyErrorCode.PARTY_NOT_FOUND));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 회원인 경우 MEMBER_NOT_FOUND 예외 발생")
        void fail_createInvitation_memberNotFound() {
            // given
            Long partyId = 1L;
            Party party = PartyFixture.createParty("모임명", 1L, null);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(memberRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> partyCommandService.createInvitation(partyId, 1L, 1L))
                    .isInstanceOf(MemberException.class)
                    .satisfies(e -> assertThat(((MemberException) e).getCode()).isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("actionInvitation")
    class ActionInvitation {

        @Test
        @DisplayName("성공 - 초대받은 멤버가 승인하면 모임 멤버로 추가되고 알림이 발생한다")
        void success_actionInvitation_approve() {
            // given
            Long invitationId = 100L;
            Long ownerId = 10L;
            Long inviteeId = 20L;

            PartyAddr addr = PartyFixture.createPartyAddr("서울", "강남");
            Member owner = MemberFixture.createMember("모임장", Gender.MALE, Level.A, ownerId);
            ReflectionTestUtils.setField(owner, "id", ownerId);
            Member invitee = MemberFixture.createMember("초대대상", Gender.FEMALE, Level.B, inviteeId);
            ReflectionTestUtils.setField(invitee, "id", inviteeId);
            Party party = PartyFixture.createParty("모임명", ownerId, addr);
            ReflectionTestUtils.setField(party, "id", 1L);

            PartyInvitation invitation = PartyInvitation.create(party, owner, invitee);
            ReflectionTestUtils.setField(invitation, "id", invitationId);

            PartyInviteActionDTO.Request request = new PartyInviteActionDTO.Request(RequestAction.APPROVE);

            given(partyInvitationRepository.findById(invitationId)).willReturn(Optional.of(invitation));
            given(memberRepository.findById(inviteeId)).willReturn(Optional.of(invitee));
            given(memberPartyRepository.existsByPartyAndMember(party, invitee)).willReturn(false);

            // when
            partyCommandService.actionInvitation(inviteeId, request, invitationId);

            // then
            assertThat(invitation.getStatus()).isEqualTo(RequestStatus.APPROVED);
            verify(chatRoomService).joinPartyChatRoom(party.getId(), invitee);
            verify(applicationEventPublisher).publishEvent(any(PartyMemberJoinedEvent.class));
            verify(notificationCommandService).createNotification(any());
        }

        @Test
        @DisplayName("성공 - 초대받은 멤버가 거절하면 상태만 REJECTED로 바뀌고 사이드이펙트가 없다")
        void success_actionInvitation_reject() {
            // given
            Long invitationId = 100L;
            Long ownerId = 10L;
            Long inviteeId = 20L;

            PartyAddr addr = PartyFixture.createPartyAddr("서울", "강남");
            Member owner = MemberFixture.createMember("모임장", Gender.MALE, Level.A, ownerId);
            ReflectionTestUtils.setField(owner, "id", ownerId);
            Member invitee = MemberFixture.createMember("초대대상", Gender.FEMALE, Level.B, inviteeId);
            ReflectionTestUtils.setField(invitee, "id", inviteeId);
            Party party = PartyFixture.createParty("모임명", ownerId, addr);
            ReflectionTestUtils.setField(party, "id", 1L);

            PartyInvitation invitation = PartyInvitation.create(party, owner, invitee);
            ReflectionTestUtils.setField(invitation, "id", invitationId);

            PartyInviteActionDTO.Request request = new PartyInviteActionDTO.Request(RequestAction.REJECT);

            given(partyInvitationRepository.findById(invitationId)).willReturn(Optional.of(invitation));
            given(memberRepository.findById(inviteeId)).willReturn(Optional.of(invitee));
            given(memberPartyRepository.existsByPartyAndMember(party, invitee)).willReturn(false);

            // when
            partyCommandService.actionInvitation(inviteeId, request, invitationId);

            // then
            assertThat(invitation.getStatus()).isEqualTo(RequestStatus.REJECTED);
            verifyNoInteractions(chatRoomService);
            verifyNoInteractions(applicationEventPublisher);
            verifyNoInteractions(notificationCommandService);
        }

        @Test
        @DisplayName("실패 - 초대받은 사람이 아닌 제3자가 처리하려 하면 NOT_YOUR_INVITATION 발생")
        void fail_actionInvitation_notYourInvitation() {
            // given
            Long invitationId = 100L;
            Long ownerId = 10L;
            Long inviteeId = 20L;
            Long otherId = 99L;

            PartyAddr addr = PartyFixture.createPartyAddr("서울", "강남");
            Member owner = MemberFixture.createMember("모임장", Gender.MALE, Level.A, ownerId);
            ReflectionTestUtils.setField(owner, "id", ownerId);
            Member invitee = MemberFixture.createMember("초대대상", Gender.FEMALE, Level.B, inviteeId);
            ReflectionTestUtils.setField(invitee, "id", inviteeId);
            Member other = MemberFixture.createMember("제3자", Gender.MALE, Level.C, otherId);
            ReflectionTestUtils.setField(other, "id", otherId);
            Party party = PartyFixture.createParty("모임명", ownerId, addr);
            ReflectionTestUtils.setField(party, "id", 1L);

            PartyInvitation invitation = PartyInvitation.create(party, owner, invitee);
            ReflectionTestUtils.setField(invitation, "id", invitationId);

            PartyInviteActionDTO.Request request = new PartyInviteActionDTO.Request(RequestAction.APPROVE);

            given(partyInvitationRepository.findById(invitationId)).willReturn(Optional.of(invitation));
            given(memberRepository.findById(otherId)).willReturn(Optional.of(other));

            // when & then
            PartyException exception = assertThrows(PartyException.class,
                    () -> partyCommandService.actionInvitation(otherId, request, invitationId));
            assertThat(exception.getCode()).isEqualTo(PartyErrorCode.NOT_YOUR_INVITATION);
        }

        @Test
        @DisplayName("실패 - 이미 처리된 초대를 다시 처리하려 할 때 INVITATION_ALREADY_ACTIONS 발생")
        void fail_actionInvitation_alreadyActions() {
            // given
            Long invitationId = 100L;
            Long ownerId = 10L;
            Long inviteeId = 20L;

            PartyAddr addr = PartyFixture.createPartyAddr("서울", "강남");
            Member owner = MemberFixture.createMember("모임장", Gender.MALE, Level.A, ownerId);
            ReflectionTestUtils.setField(owner, "id", ownerId);
            Member invitee = MemberFixture.createMember("초대대상", Gender.FEMALE, Level.B, inviteeId);
            ReflectionTestUtils.setField(invitee, "id", inviteeId);
            Party party = PartyFixture.createParty("모임명", ownerId, addr);
            ReflectionTestUtils.setField(party, "id", 1L);

            // 이미 승인된 초대
            PartyInvitation invitation = PartyInvitation.create(party, owner, invitee);
            ReflectionTestUtils.setField(invitation, "id", invitationId);
            invitation.updateStatus(RequestStatus.APPROVED);

            PartyInviteActionDTO.Request request = new PartyInviteActionDTO.Request(RequestAction.REJECT);

            given(partyInvitationRepository.findById(invitationId)).willReturn(Optional.of(invitation));
            given(memberRepository.findById(inviteeId)).willReturn(Optional.of(invitee));
            given(memberPartyRepository.existsByPartyAndMember(party, invitee)).willReturn(false);

            // when & then
            PartyException exception = assertThrows(PartyException.class,
                    () -> partyCommandService.actionInvitation(inviteeId, request, invitationId));
            assertThat(exception.getCode()).isEqualTo(PartyErrorCode.INVITATION_ALREADY_ACTIONS);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 회원인 경우 MEMBER_NOT_FOUND 예외 발생")
        void fail_actionInvitation_memberNotFound() {
            // given
            Long invitationId = 100L;
            Party party = PartyFixture.createParty("모임명", 1L, null);
            Member owner = MemberFixture.createMember("모임장", Gender.MALE, Level.A, 1L);
            Member invitee = MemberFixture.createMember("초대대상", Gender.FEMALE, Level.B, 20L);
            PartyInvitation invitation = PartyInvitation.create(party, owner, invitee);

            PartyInviteActionDTO.Request request = new PartyInviteActionDTO.Request(RequestAction.APPROVE);
            given(partyInvitationRepository.findById(invitationId)).willReturn(Optional.of(invitation));
            given(memberRepository.findById(20L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> partyCommandService.actionInvitation(20L, request, invitationId))
                    .isInstanceOf(MemberException.class)
                    .satisfies(e -> assertThat(((MemberException) e).getCode()).isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("addKeyword")
    class AddKeyword {

        @Test
        @DisplayName("성공 - 모임장이 유효한 키워드 목록을 모임에 추가한다")
        void success_addKeyword() {
            // given
            Long partyId = 1L;
            Long ownerId = 10L;

            PartyAddr addr = PartyFixture.createPartyAddr("서울", "강남");
            Party party = PartyFixture.createParty("모임명", ownerId, addr);
            ReflectionTestUtils.setField(party, "id", partyId);

            PartyKeywordDTO.Request request = new PartyKeywordDTO.Request(
                    List.of("친목", "가입비 무료")
            );

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));

            // when
            partyCommandService.addKeyword(partyId, ownerId, request);

            // then
            assertThat(party.getKeywords()).hasSize(2);
        }

        @Test
        @DisplayName("실패 - 모임장이 아닌 사용자가 키워드를 추가하면 INSUFFICIENT_PERMISSION 발생")
        void fail_addKeyword_notOwner() {
            // given
            Long partyId = 1L;
            Long ownerId = 10L;
            Long nonOwnerId = 99L;

            PartyAddr addr = PartyFixture.createPartyAddr("서울", "강남");
            Party party = PartyFixture.createParty("모임명", ownerId, addr);
            ReflectionTestUtils.setField(party, "id", partyId);

            PartyKeywordDTO.Request request = new PartyKeywordDTO.Request(List.of("친목"));

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));

            // when & then
            PartyException exception = assertThrows(PartyException.class,
                    () -> partyCommandService.addKeyword(partyId, nonOwnerId, request));
            assertThat(exception.getCode()).isEqualTo(PartyErrorCode.INSUFFICIENT_PERMISSION);
        }

        @Test
        @DisplayName("실패 - 유효하지 않은 키워드 문자열을 전달하면 INVALID_KEYWORD 발생")
        void fail_addKeyword_invalidKeyword() {
            // given
            Long partyId = 1L;
            Long ownerId = 10L;

            PartyAddr addr = PartyFixture.createPartyAddr("서울", "강남");
            Party party = PartyFixture.createParty("모임명", ownerId, addr);
            ReflectionTestUtils.setField(party, "id", partyId);

            PartyKeywordDTO.Request request = new PartyKeywordDTO.Request(List.of("존재하지않는키워드"));

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));

            // when & then
            PartyException exception = assertThrows(PartyException.class,
                    () -> partyCommandService.addKeyword(partyId, ownerId, request));
            assertThat(exception.getCode()).isEqualTo(PartyErrorCode.INVALID_KEYWORD);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 파티인 경우 PARTY_NOT_FOUND 예외 발생")
        void fail_addKeyword_partyNotFound() {
            // given
            PartyKeywordDTO.Request request = new PartyKeywordDTO.Request(List.of("새키워드"));
            given(partyRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> partyCommandService.addKeyword(999L, 10L, request))
                    .isInstanceOf(PartyException.class)
                    .satisfies(e -> assertThat(((PartyException) e).getCode()).isEqualTo(PartyErrorCode.PARTY_NOT_FOUND));
        }
    }
}
