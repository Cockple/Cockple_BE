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
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.party.converter.PartyConverter;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.domain.PartyAddr;
import umc.cockple.demo.domain.party.domain.PartyJoinRequest;
import umc.cockple.demo.domain.party.dto.*;
import umc.cockple.demo.domain.party.enums.ParticipationType;
import umc.cockple.demo.domain.party.enums.RequestStatus;
import umc.cockple.demo.domain.party.events.PartyMemberJoinedEvent;
import umc.cockple.demo.domain.party.exception.PartyErrorCode;
import umc.cockple.demo.domain.party.exception.PartyException;
import umc.cockple.demo.domain.party.repository.PartyAddrRepository;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PartyCommandServiceTest {

    @InjectMocks
    private PartyCommandServiceImpl partyCommandService;

    @Mock
    private PartyRepository partyRepository;
    @Mock
    private MemberRepository memberRepository;
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

            MemberParty memberParty = MemberFixture.createMemberParty(party, member, Role.party_MEMBER);

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

            MemberParty subManagerParty = MemberFixture.createMemberParty(party, subManager, Role.party_SUBMANAGER);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(memberRepository.findById(subManagerId)).willReturn(Optional.of(subManager));
            given(memberPartyRepository.findByPartyIdAndRole(partyId, Role.party_SUBMANAGER))
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
                    .status(umc.cockple.demo.domain.party.enums.PartyStatus.ACTIVE)
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
                    .status(umc.cockple.demo.domain.party.enums.PartyStatus.ACTIVE)
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
    }
}
