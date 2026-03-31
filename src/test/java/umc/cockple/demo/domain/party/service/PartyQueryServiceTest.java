package umc.cockple.demo.domain.party.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;
import umc.cockple.demo.domain.bookmark.repository.PartyBookmarkRepository;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.file.service.FileService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberAddr;
import umc.cockple.demo.domain.member.domain.MemberParty;
import umc.cockple.demo.domain.member.repository.MemberAddrRepository;
import umc.cockple.demo.domain.member.repository.MemberExerciseRepository;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.party.converter.PartyConverter;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.domain.PartyAddr;
import umc.cockple.demo.domain.party.domain.PartyJoinRequest;
import umc.cockple.demo.domain.party.dto.*;
import umc.cockple.demo.domain.party.enums.RequestStatus;
import umc.cockple.demo.domain.party.exception.PartyErrorCode;
import umc.cockple.demo.domain.party.exception.PartyException;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PartyQueryServiceTest {

    @InjectMocks
    private PartyQueryServiceImpl partyQueryService;

    @Mock
    private PartyRepository partyRepository;
    @Mock
    private MemberRepository memberRepository;

    private PartyConverter partyConverter;

    @Mock
    private MemberPartyRepository memberPartyRepository;
    @Mock
    private MemberExerciseRepository memberExerciseRepository;
    @Mock
    private ExerciseRepository exerciseRepository;
    @Mock
    private PartyBookmarkRepository partyBookmarkRepository;
    @Mock
    private MemberAddrRepository memberAddrRepository;
    @Mock
    private FileService fileService;
    @Mock
    private PartyJoinRequestRepository partyJoinRequestRepository;

    @BeforeEach
    void setUp() {
        partyConverter = new PartyConverter(fileService);
        ReflectionTestUtils.setField(partyQueryService, "partyConverter", partyConverter);
    }

    @Nested
    @DisplayName("getPartyMembers")
    class GetPartyMembers {

        @Test
        @DisplayName("성공 - 모임의 멤버들을 역할별로 성공적으로 조회한다.")
        void success_getPartyMembers() {
            // given
            Long partyId = 1L;
            Long currentMemberId = 10L;

            PartyAddr addr = PartyFixture.createPartyAddr("서울특별시", "강남구");
            Party party = PartyFixture.createParty("테스트 모임", 10L, addr);
            ReflectionTestUtils.setField(party, "id", partyId);

            Member manager = MemberFixture.createMember("모임장", Gender.MALE, Level.A, 1001L);
            Member subManager = MemberFixture.createMember("부모임장", Gender.FEMALE, Level.B, 1002L);
            Member normalMember = MemberFixture.createMember("일반멤버", Gender.MALE, Level.C, 1003L);

            ReflectionTestUtils.setField(manager, "id", 10L);
            ReflectionTestUtils.setField(subManager, "id", 20L);
            ReflectionTestUtils.setField(normalMember, "id", 30L);

            MemberParty mp1 = MemberFixture.createMemberParty(party, manager, Role.PARTY_MANAGER);
            MemberParty mp2 = MemberFixture.createMemberParty(party, subManager, Role.PARTY_SUBMANAGER);
            MemberParty mp3 = MemberFixture.createMemberParty(party, normalMember, Role.PARTY_MEMBER);
            List<MemberParty> memberParties = List.of(mp1, mp2, mp3);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(memberPartyRepository.findAllByPartyIdWithMember(partyId)).willReturn(memberParties);
            given(memberExerciseRepository.findLastExerciseDateByMemberIdsAndPartyId(anyList(),
                    eq(partyId)))
                    .willReturn(List.of());

            // when
            PartyMemberDTO.Response result = partyQueryService.getPartyMembers(partyId, currentMemberId);

            // then
            assertThat(result.members()).hasSize(3);
            assertThat(result.summary().totalCount()).isEqualTo(3);
            assertThat(result.summary().maleCount()).isEqualTo(2);
            assertThat(result.summary().femaleCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("성공 - 멤버 목록과 마지막 운동일을 함께 반환한다")
        void success_getPartyMembers_withExerciseHistory() {
            // given
            Long partyId = 1L;
            Long currentMemberId = 10L;

            PartyAddr addr = PartyFixture.createPartyAddr("서울특별시", "강남구");
            Party party = PartyFixture.createParty("테스트 모임", 10L, addr);
            ReflectionTestUtils.setField(party, "id", partyId);
            Member manager = MemberFixture.createMember("매니저", Gender.MALE, Level.A, 1001L);
            Member member1 = MemberFixture.createMember("멤버1", Gender.FEMALE, Level.A, 1002L);
            ReflectionTestUtils.setField(manager, "id", 10L);
            ReflectionTestUtils.setField(member1, "id", 20L);

            MemberParty mp1 = MemberFixture.createMemberParty(party, manager, Role.PARTY_MANAGER);
            MemberParty mp2 = MemberFixture.createMemberParty(party, member1, Role.PARTY_MEMBER);
            List<MemberParty> memberParties = List.of(mp1, mp2);

            LocalDate lastDate = LocalDate.of(2025, 1, 10);
            List<Object[]> rawResult = List.<Object[]>of(new Object[]{20L, lastDate});

            PartyMemberDTO.Response expected = PartyMemberDTO.Response.builder()
                    .summary(PartyMemberDTO.Summary.builder()
                            .totalCount(2).maleCount(1).femaleCount(1).build())
                    .members(List.of())
                    .build();

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(memberPartyRepository.findAllByPartyIdWithMember(partyId)).willReturn(memberParties);
            given(memberExerciseRepository.findLastExerciseDateByMemberIdsAndPartyId(
                    List.of(10L, 20L), partyId)).willReturn(rawResult);

            // when
            PartyMemberDTO.Response result = partyQueryService.getPartyMembers(partyId, currentMemberId);

            // then
            assertThat(result.summary().totalCount()).isEqualTo(2);
            assertThat(result.members()).hasSize(2);
            // 마지막 운동일 확인 (멤버1 id: 20L)
            assertThat(result.members().stream()
                    .filter(m -> m.memberId().equals(20L))
                    .findFirst()
                    .get().lastExerciseDate()).isEqualTo(lastDate);
        }

        @Test
        @DisplayName("성공 - 운동 기록이 없는 멤버는 빈 Map이 converter에 전달된다")
        void success_getPartyMembers_noExerciseHistory() {
            // given
            Long partyId = 1L;
            Long currentMemberId = 10L;

            PartyAddr addr = PartyFixture.createPartyAddr("서울특별시", "강남구");
            Party party = PartyFixture.createParty("테스트 모임", 10L, addr);
            ReflectionTestUtils.setField(party, "id", partyId);
            Member manager = MemberFixture.createMember("매니저", Gender.MALE, Level.A, 1001L);
            ReflectionTestUtils.setField(manager, "id", 10L);
            MemberParty mp = MemberFixture.createMemberParty(party, manager, Role.PARTY_MANAGER);
            List<MemberParty> memberParties = List.of(mp);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(memberPartyRepository.findAllByPartyIdWithMember(partyId)).willReturn(memberParties);
            given(memberExerciseRepository.findLastExerciseDateByMemberIdsAndPartyId(
                    List.of(10L), partyId)).willReturn(List.of());

            // when
            PartyMemberDTO.Response result = partyQueryService.getPartyMembers(partyId, currentMemberId);

            // then
            assertThat(result.members()).hasSize(1);
            assertThat(result.members().get(0).lastExerciseDate()).isNull();
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 파티면 PartyException을 던진다")
        void fail_getPartyMembers_partyNotFound() {
            // given
            given(partyRepository.findById(99L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> partyQueryService.getPartyMembers(99L, 1L))
                    .isInstanceOf(PartyException.class)
                    .satisfies(e -> assertThat(((PartyException) e).getCode())
                            .isEqualTo(PartyErrorCode.PARTY_NOT_FOUND));
        }

        @Test
        @DisplayName("실패 - 비활성화된 파티면 PartyException을 던진다")
        void fail_getPartyMembers_partyInactive() {
            // given
            PartyAddr addr = PartyFixture.createPartyAddr("서울특별시", "강남구");
            Party inactiveParty = PartyFixture.createParty("테스트 모임", 10L, addr);
            ReflectionTestUtils.setField(inactiveParty, "id", 1L);
            inactiveParty.delete();

            given(partyRepository.findById(1L)).willReturn(Optional.of(inactiveParty));

            // when & then
            assertThatThrownBy(() -> partyQueryService.getPartyMembers(1L, 1L))
                    .isInstanceOf(PartyException.class)
                    .satisfies(e -> assertThat(((PartyException) e).getCode())
                            .isEqualTo(PartyErrorCode.PARTY_IS_DELETED));
        }
    }

    @Nested
    @DisplayName("getMyParties")
    class GetMyParties {

        @Test
        @DisplayName("성공 - 내 모임 목록과 부가 정보(운동 횟수, 다음 운동 정보, 북마크 여부)를 조합하여 반환한다")
        void success_getMyParties() {
            // given
            Long memberId = 10L;
            Pageable pageable = PageRequest.of(0, 10);

            PartyAddr addr = PartyFixture.createPartyAddr("서울특별시", "강남구");
            Party party = PartyFixture.createParty("테스트 모임", 10L, addr);
            ReflectionTestUtils.setField(party, "id", 1L);

            Slice<Party> partySlice = new SliceImpl<>(List.of(party), pageable, false);

            PartyDTO.Response expectedResponse = PartyDTO.Response.builder()
                    .partyId(1L)
                    .partyName("테스트 모임")
                    .totalExerciseCount(5)
                    .nextExerciseInfo("05.01 오전 운동")
                    .isBookmarked(true)
                    .build();

            given(partyRepository.findMyParty(eq(memberId), eq(false), any(Pageable.class)))
                    .willReturn(partySlice);
            given(exerciseRepository.findTotalExerciseCountsByPartyIds(List.of(1L)))
                    .willReturn(List.of());
            given(exerciseRepository.findUpcomingExercisesByPartyIds(List.of(1L)))
                    .willReturn(List.of());
            given(partyBookmarkRepository.findAllPartyIdsByMemberId(memberId))
                    .willReturn(Set.of(1L));
            // when
            Slice<PartyDTO.Response> result = partyQueryService.getMyParties(memberId, false, "최신순",
                    pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).partyName()).isEqualTo("테스트 모임");
            assertThat(result.getContent().get(0).isBookmarked()).isTrue();

            verify(partyRepository).findMyParty(eq(memberId), eq(false), any(Pageable.class));
        }

        @Test
        @DisplayName("성공 - 사용자가 가입한 모임 목록을 오래된 순으로 페이징하여 반환한다")
        void success_getMyParties_oldest() {
            // given
            Long memberId = 10L;
            Pageable pageable = PageRequest.of(0, 10);

            PartyAddr addr = PartyFixture.createPartyAddr("서울특별시", "강남구");

            Party party1 = PartyFixture.createParty("테스트 모임1", 10L, addr);
            ReflectionTestUtils.setField(party1, "id", 1L);
            Party party2 = PartyFixture.createParty("테스트 모임2", 10L, addr);
            ReflectionTestUtils.setField(party2, "id", 2L);

            // 오래된 순: party1, party2 순서
            Slice<Party> partySlice = new SliceImpl<>(List.of(party1, party2), pageable, false);

            given(partyRepository.findMyParty(eq(memberId), eq(false), any(Pageable.class)))
                    .willReturn(partySlice);
            given(exerciseRepository.findTotalExerciseCountsByPartyIds(List.of(1L, 2L)))
                    .willReturn(List.of());
            given(exerciseRepository.findUpcomingExercisesByPartyIds(List.of(1L, 2L)))
                    .willReturn(List.of());
            given(partyBookmarkRepository.findAllPartyIdsByMemberId(memberId))
                    .willReturn(Set.of());

            // when
            Slice<PartyDTO.Response> result = partyQueryService.getMyParties(memberId, false,
                    "오래된 순", pageable);

            // then
            assertThat(result.getContent()).hasSize(2);
            verify(partyRepository).findMyParty(eq(memberId), eq(false), any(Pageable.class));
        }

        @Test
        @DisplayName("성공 - 사용자가 가입한 모임 목록을 운동 많은 순으로 페이징하여 반환한다")
        void success_getMyParties_exerciseCount() {
            // given
            Long memberId = 10L;
            Pageable pageable = PageRequest.of(0, 10);

            PartyAddr addr = PartyFixture.createPartyAddr("서울특별시", "강남구");

            Party party1 = PartyFixture.createParty("운동많은모임", 10L, addr);
            ReflectionTestUtils.setField(party1, "id", 1L);
            Party party2 = PartyFixture.createParty("운동적은모임", 10L, addr);
            ReflectionTestUtils.setField(party2, "id", 2L);

            // 운동 많은 순: party1, party2 순서
            Slice<Party> partySlice = new SliceImpl<>(List.of(party1, party2), pageable, false);

            given(partyRepository.findMyParty(eq(memberId), eq(false), any(Pageable.class)))
                    .willReturn(partySlice);
            given(exerciseRepository.findTotalExerciseCountsByPartyIds(List.of(1L, 2L)))
                    .willReturn(List.of());
            given(exerciseRepository.findUpcomingExercisesByPartyIds(List.of(1L, 2L)))
                    .willReturn(List.of());
            given(partyBookmarkRepository.findAllPartyIdsByMemberId(memberId))
                    .willReturn(Set.of());

            // when
            Slice<PartyDTO.Response> result = partyQueryService.getMyParties(memberId, false,
                    "운동 많은 순", pageable);

            // then
            assertThat(result.getContent()).hasSize(2);
            verify(partyRepository).findMyParty(eq(memberId), eq(false), any(Pageable.class));
        }

        @Test
        @DisplayName("실패 - 유효하지 않은 정렬 기준을 전달하면 INVALID_ORDER_TYPE 발생")
        void fail_getMyParties_invalidSort() {
            // given
            Long memberId = 10L;
            Pageable pageable = PageRequest.of(0, 10);

            // when & then
            assertThatThrownBy(() -> partyQueryService.getMyParties(memberId, false, "존재하지않는정렬", pageable))
                    .isInstanceOf(PartyException.class)
                    .satisfies(e -> assertThat(((PartyException) e).getCode())
                            .isEqualTo(PartyErrorCode.INVALID_ORDER_TYPE));
        }
    }

    @Nested
    @DisplayName("getSimpleMyParties")
    class GetSimpleMyParties {

        @Test
        @DisplayName("성공 - 유효한 회원 ID가 주어지면 가입한 모임의 간략화된 목록을 반환한다")
        void success_getSimpleMyParties() {
            // given
            Long memberId = 1L;
            Pageable pageable = PageRequest.of(0, 10);

            Member member = MemberFixture.createMember("사용자", Gender.MALE, Level.A, 1001L);
            ReflectionTestUtils.setField(member, "id", memberId);

            PartyAddr addr = PartyFixture.createPartyAddr("서울특별시", "강남구");
            Party party = PartyFixture.createParty("테스트 모임", 10L, addr);
            ReflectionTestUtils.setField(party, "id", 10L);

            MemberParty memberParty = MemberFixture.createMemberParty(party, member, Role.PARTY_MEMBER);

            Slice<MemberParty> memberPartySlice = new SliceImpl<>(List.of(memberParty), pageable, false);

            PartySimpleDTO.Response expectedResponse = PartySimpleDTO.Response.builder()
                    .partyId(10L)
                    .partyName("테스트 모임")
                    .build();

            given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
            given(memberPartyRepository.findByMember(member, pageable)).willReturn(memberPartySlice);

            // when
            Slice<PartySimpleDTO.Response> result = partyQueryService.getSimpleMyParties(memberId,
                    pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).partyName()).isEqualTo("테스트 모임");

            verify(memberRepository).findById(memberId);
            verify(memberPartyRepository).findByMember(member, pageable);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 회원일 경우 MemberException을 던진다")
        void fail_getSimpleMyParties_memberNotFound() {
            // given
            Long invalidMemberId = 999L;
            Pageable pageable = PageRequest.of(0, 10);

            given(memberRepository.findById(invalidMemberId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> partyQueryService.getSimpleMyParties(invalidMemberId, pageable))
                    .isInstanceOf(umc.cockple.demo.domain.member.exception.MemberException.class)
                    .satisfies(e -> assertThat(
                            ((umc.cockple.demo.domain.member.exception.MemberException) e)
                                    .getCode())
                            .isEqualTo(umc.cockple.demo.domain.member.exception.MemberErrorCode.MEMBER_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("getRecommendedParties")
    class GetRecommendedParties {

        @Test
        @DisplayName("성공 - Cockple 추천 모드 시 유저 정보(주소, 생년월일, 키워드)를 기반으로 추천 목록을 반환한다")
        void success_getRecommendedParties_cockpleRecommend() {
            // given
            Long memberId = 1L;
            Pageable pageable = PageRequest.of(0, 10);
            PartyFilterDTO.Request filter = PartyFilterDTO.Request.builder().build();

            Member member = MemberFixture.createMember("매니저", Gender.MALE, Level.A, 1001L,
                    LocalDate.of(1995, 1, 1));
            ReflectionTestUtils.setField(member, "id", memberId);

            MemberAddr addr = MemberAddr.builder()
                    .member(member)
                    .addr1("서울특별시")
                    .isMain(true)
                    .build();

            Party suggestedParty = PartyFixture.createParty("추천 모임", 2L,
                    PartyFixture.createPartyAddr("서울특별시", "강남구"));
            ReflectionTestUtils.setField(suggestedParty, "id", 100L);

            given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
            given(memberAddrRepository.findByMemberAndIsMain(member, true)).willReturn(Optional.of(addr));
            given(partyRepository.findRecommendedParties(anyString(), anyInt(), any(), any(), anyLong()))
                    .willReturn(List.of(suggestedParty));
            given(partyBookmarkRepository.findAllPartyIdsByMemberId(memberId)).willReturn(Set.of());

            // when
            Slice<PartyDTO.Response> result = partyQueryService.getRecommendedParties(memberId, true,
                    filter, "최신순", pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).partyName()).isEqualTo("추천 모임");
            verify(partyRepository).findRecommendedParties(eq("서울특별시"), eq(1995), eq(Gender.MALE),
                    eq(Level.A), eq(memberId));
        }

        @Test
        @DisplayName("성공 - 필터 모드 시 설정한 필터 조건(addr1, addr2 등)에 맞는 모임 목록을 반환한다")
        void success_getRecommendedParties_filterMode() {
            // given
            Long memberId = 1L;
            Pageable pageable = PageRequest.of(0, 10);
            PartyFilterDTO.Request filter = PartyFilterDTO.Request.builder()
                    .addr1("서울특별시")
                    .addr2("강남구")
                    .build();

            Party filteredParty = PartyFixture.createParty("필터 모임", 2L,
                    PartyFixture.createPartyAddr("서울특별시", "강남구"));
            ReflectionTestUtils.setField(filteredParty, "id", 200L);
            Slice<Party> partySlice = new SliceImpl<>(List.of(filteredParty), pageable, false);

            given(partyRepository.searchParties(eq(memberId), eq(filter), any(Pageable.class)))
                    .willReturn(partySlice);
            given(partyBookmarkRepository.findAllPartyIdsByMemberId(memberId)).willReturn(Set.of());

            // when
            Slice<PartyDTO.Response> result = partyQueryService.getRecommendedParties(memberId, false,
                    filter, "최신순", pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).partyName()).isEqualTo("필터 모임");
            verify(partyRepository).searchParties(eq(memberId), eq(filter), any(Pageable.class));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 회원 ID로 추천 요청 시 MEMBER_NOT_FOUND이 발생한다")
        void fail_getRecommendedParties_memberNotFound() {
            // given
            Long memberId = 999L;
            Pageable pageable = PageRequest.of(0, 10);
            PartyFilterDTO.Request filter = PartyFilterDTO.Request.builder().build();

            given(memberRepository.findById(memberId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> partyQueryService.getRecommendedParties(memberId, true, filter, "최신순",
                    pageable))
                    .isInstanceOf(umc.cockple.demo.domain.member.exception.MemberException.class)
                    .satisfies(e -> assertThat(
                            ((umc.cockple.demo.domain.member.exception.MemberException) e)
                                    .getCode())
                            .isEqualTo(umc.cockple.demo.domain.member.exception.MemberErrorCode.MEMBER_NOT_FOUND));
        }

        @Test
        @DisplayName("실패 - 대표 주소가 설정되지 않은 회원이 추천 요청 시 MAIN_ADDRESS_NULL이 발생한다")
        void fail_getRecommendedParties_mainAddressNotFound() {
            // given
            Long memberId = 1L;
            Pageable pageable = PageRequest.of(0, 10);
            PartyFilterDTO.Request filter = PartyFilterDTO.Request.builder().build();

            Member member = MemberFixture.createMember("매니저", Gender.MALE, Level.A, 1001L);
            ReflectionTestUtils.setField(member, "id", memberId);

            given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
            given(memberAddrRepository.findByMemberAndIsMain(member, true)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> partyQueryService.getRecommendedParties(memberId, true, filter, "최신순",
                    pageable))
                    .isInstanceOf(umc.cockple.demo.domain.member.exception.MemberException.class)
                    .satisfies(e -> assertThat(
                            ((umc.cockple.demo.domain.member.exception.MemberException) e)
                                    .getCode())
                            .isEqualTo(umc.cockple.demo.domain.member.exception.MemberErrorCode.MAIN_ADDRESS_NULL));
        }
    }

    @Nested
    @DisplayName("getPartyDetails")
    class GetPartyDetails {

        @Test
        @DisplayName("성공 - 모임 상세 정보를 정상적으로 조회한다 (비회원, 신청 전)")
        void success_getPartyDetails_nonMember() {
            // given
            Long partyId = 1L;
            Long memberId = 10L;

            PartyAddr addr = PartyFixture.createPartyAddr("서울", "강남");
            Party party = PartyFixture.createParty("상세 모임", 11L, addr);
            ReflectionTestUtils.setField(party, "id", partyId);
            Member member = MemberFixture.createMember("사용자", Gender.MALE, Level.A, 1000L);
            ReflectionTestUtils.setField(member, "id", memberId);

            PartyDetailDTO.Response expected = PartyDetailDTO.Response.builder()
                    .partyId(partyId)
                    .partyName("상세 모임")
                    .memberStatus("NOT_MEMBER")
                    .hasPendingJoinRequest(false)
                    .build();

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
            given(memberPartyRepository.findByPartyAndMember(party, member)).willReturn(Optional.empty());
            given(partyBookmarkRepository.existsByMemberAndParty(member, party)).willReturn(false);
            given(partyJoinRequestRepository.existsByPartyAndMemberAndStatus(party, member,
                    RequestStatus.PENDING)).willReturn(false);

            // when
            PartyDetailDTO.Response result = partyQueryService.getPartyDetails(partyId, memberId);

            // then
            assertThat(result.partyId()).isEqualTo(partyId);
            assertThat(result.partyName()).isEqualTo("상세 모임");
            assertThat(result.memberStatus()).isEqualTo("NOT_MEMBER");
            assertThat(result.hasPendingJoinRequest()).isFalse();
            assertThat(result.isBookmarked()).isFalse();
            verify(partyRepository).findById(partyId);
        }

        @Test
        @DisplayName("성공 - 모임원인 경우 memberStatus가 MEMBER로 반환된다")
        void success_getPartyDetails_member() {
            // given
            Long partyId = 1L;
            Long memberId = 10L;

            PartyAddr addr = PartyFixture.createPartyAddr("서울", "강남");
            Party party = PartyFixture.createParty("상세 모임", 11L, addr);
            ReflectionTestUtils.setField(party, "id", partyId);
            Member member = MemberFixture.createMember("사용자", Gender.MALE, Level.A, 1000L);
            ReflectionTestUtils.setField(member, "id", memberId);

            MemberParty memberParty = MemberFixture.createMemberParty(party, member, Role.PARTY_MEMBER);
            PartyDetailDTO.Response expected = PartyDetailDTO.Response.builder()
                    .partyId(partyId)
                    .memberStatus("MEMBER")
                    .memberRole("PARTY_MEMBER")
                    .build();

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
            given(memberPartyRepository.findByPartyAndMember(party, member))
                    .willReturn(Optional.of(memberParty));
            given(partyBookmarkRepository.existsByMemberAndParty(member, party)).willReturn(true);

            // when
            PartyDetailDTO.Response result = partyQueryService.getPartyDetails(partyId, memberId);

            // then
            assertThat(result.memberStatus()).isEqualTo("MEMBER");
            assertThat(result.memberRole()).isEqualTo("PARTY_MEMBER");
            assertThat(result.hasPendingJoinRequest()).isNull(); // 멤버이므로 null 반환
            assertThat(result.isBookmarked()).isTrue();
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 모임 조회 시 PARTY_NOT_FOUND이 발생한다")
        void fail_getPartyDetails_partyNotFound() {
            // given
            given(partyRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> partyQueryService.getPartyDetails(999L, 1L))
                    .isInstanceOf(PartyException.class)
                    .satisfies(e -> assertThat(((PartyException) e).getCode())
                            .isEqualTo(PartyErrorCode.PARTY_NOT_FOUND));
        }

        @Test
        @DisplayName("실패 - 삭제된 모임 조회 시 PARTY_IS_DELETED이 발생한다")
        void fail_getPartyDetails_partyDeleted() {
            // given
            PartyAddr addr = PartyFixture.createPartyAddr("서울", "강남");
            Party party = PartyFixture.createParty("삭제된 모임", 11L, addr);
            party.delete();
            given(partyRepository.findById(1L)).willReturn(Optional.of(party));
            given(memberRepository.findById(1L)).willReturn(
                    Optional.of(MemberFixture.createMember("테스터", Gender.MALE, Level.A, 1L)));

            // when & then
            assertThatThrownBy(() -> partyQueryService.getPartyDetails(1L, 1L))
                    .isInstanceOf(PartyException.class)
                    .satisfies(e -> assertThat(((PartyException) e).getCode())
                            .isEqualTo(PartyErrorCode.PARTY_IS_DELETED));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 회원인 경우 MEMBER_NOT_FOUND 예외 발생")
        void fail_getPartyDetails_memberNotFound() {
            // given
            Long partyId = 1L;
            Party party = PartyFixture.createParty("상세 모임", 11L, null);
            ReflectionTestUtils.setField(party, "id", partyId);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(memberRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> partyQueryService.getPartyDetails(partyId, 1L))
                    .isInstanceOf(umc.cockple.demo.domain.member.exception.MemberException.class)
                    .satisfies(e -> assertThat(((umc.cockple.demo.domain.member.exception.MemberException) e).getCode())
                            .isEqualTo(umc.cockple.demo.domain.member.exception.MemberErrorCode.MEMBER_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("getJoinRequests")
    class GetJoinRequests {

        @Test
        @DisplayName("성공 - 모임장이 가입 신청 목록을 정상적으로 조회한다")
        void success_getJoinRequests() {
            // given
            Long partyId = 1L;
            Long ownerId = 10L;
            Pageable pageable = PageRequest.of(0, 10);
            String status = "PENDING";

            PartyAddr addr = PartyFixture.createPartyAddr("서울", "강남");
            Party party = PartyFixture.createParty("모임명", ownerId, addr);
            ReflectionTestUtils.setField(party, "id", partyId);

            Member owner = MemberFixture.createMember("모임장", Gender.MALE, Level.A, ownerId);
            ReflectionTestUtils.setField(owner, "id", ownerId);
            Member applicant = MemberFixture.createMember("지원자", Gender.FEMALE, Level.B, 20L);
            ReflectionTestUtils.setField(applicant, "id", 20L);

            PartyJoinRequest joinRequest = PartyJoinRequest.builder()
                    .party(party)
                    .member(applicant)
                    .status(RequestStatus.PENDING)
                    .build();
            ReflectionTestUtils.setField(joinRequest, "id", 100L);

            Slice<PartyJoinRequest> requestSlice = new SliceImpl<>(List.of(joinRequest), pageable, false);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));

            given(partyJoinRequestRepository.findByPartyAndStatus(party, RequestStatus.PENDING, pageable))
                    .willReturn(requestSlice);

            // when
            Slice<PartyJoinDTO.Response> result = partyQueryService.getJoinRequests(partyId, ownerId, status, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).joinRequestId()).isEqualTo(100L);
            verify(partyJoinRequestRepository).findByPartyAndStatus(party, RequestStatus.PENDING, pageable);
        }

        @Test
        @DisplayName("성공 - 모임장이 가입 승인된 멤버 목록(APPROVED)을 정상적으로 조회한다")
        void success_getJoinRequests_approved() {
            // given
            Long partyId = 1L;
            Long ownerId = 10L;
            Pageable pageable = PageRequest.of(0, 10);
            String status = "APPROVED";

            PartyAddr addr = PartyFixture.createPartyAddr("서울", "강남");
            Party party = PartyFixture.createParty("모임명", ownerId, addr);
            ReflectionTestUtils.setField(party, "id", partyId);

            Member applicant = MemberFixture.createMember("지원자", Gender.FEMALE, Level.B, 20L);
            ReflectionTestUtils.setField(applicant, "id", 20L);

            PartyJoinRequest joinRequest = PartyJoinRequest.builder()
                    .party(party)
                    .member(applicant)
                    .status(RequestStatus.APPROVED)
                    .build();
            ReflectionTestUtils.setField(joinRequest, "id", 101L);

            Slice<PartyJoinRequest> requestSlice = new SliceImpl<>(List.of(joinRequest), pageable, false);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));

            given(partyJoinRequestRepository.findByPartyAndStatus(party, RequestStatus.APPROVED, pageable))
                    .willReturn(requestSlice);

            // when
            Slice<PartyJoinDTO.Response> result = partyQueryService.getJoinRequests(partyId, ownerId, status, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).joinRequestId()).isEqualTo(101L);
            verify(partyJoinRequestRepository).findByPartyAndStatus(party, RequestStatus.APPROVED, pageable);
        }

        @Test
        @DisplayName("실패 - 모임장이 아닌 사용자가 조회하면 INSUFFICIENT_PERMISSION 발생")
        void fail_getJoinRequests_notOwner() {
            // given
            Long partyId = 1L;
            Long nonOwnerId = 20L;
            Pageable pageable = PageRequest.of(0, 10);
            String status = "PENDING";

            PartyAddr addr = PartyFixture.createPartyAddr("서울", "강남");
            Party party = PartyFixture.createParty("모임명", 10L, addr);
            ReflectionTestUtils.setField(party, "id", partyId);

            Member nonOwner = MemberFixture.createMember("일반멤버", Gender.FEMALE, Level.B, nonOwnerId);
            ReflectionTestUtils.setField(nonOwner, "id", nonOwnerId);
            MemberParty nonOwnerParty = MemberFixture.createMemberParty(party, nonOwner, Role.PARTY_MEMBER);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));


            // when & then
            assertThatThrownBy(() -> partyQueryService.getJoinRequests(partyId, nonOwnerId, status, pageable))
                    .isInstanceOf(PartyException.class)
                    .satisfies(e -> assertThat(((PartyException) e).getCode()).isEqualTo(PartyErrorCode.INSUFFICIENT_PERMISSION));
        }

        @Test
        @DisplayName("실패 - 잘못된 상태값을 입력하면 INVALID_REQUEST_STATUS 발생")
        void fail_getJoinRequests_invalidStatus() {
            // given
            Long partyId = 1L;
            Long ownerId = 10L;
            Pageable pageable = PageRequest.of(0, 10);
            String invalidStatus = "UNKNOWN";

            PartyAddr addr = PartyFixture.createPartyAddr("서울", "강남");
            Party party = PartyFixture.createParty("모임명", ownerId, addr);
            ReflectionTestUtils.setField(party, "id", partyId);

            Member owner = MemberFixture.createMember("모임장", Gender.MALE, Level.A, ownerId);
            ReflectionTestUtils.setField(owner, "id", ownerId);
            MemberParty ownerParty = MemberFixture.createMemberParty(party, owner, Role.PARTY_MANAGER);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));


            // when & then
            assertThatThrownBy(() -> partyQueryService.getJoinRequests(partyId, ownerId, invalidStatus, pageable))
                    .isInstanceOf(PartyException.class)
                    .satisfies(e -> assertThat(((PartyException) e).getCode()).isEqualTo(PartyErrorCode.INVALID_REQUEST_STATUS));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 파티인 경우 PARTY_NOT_FOUND 예외 발생")
        void fail_getJoinRequests_partyNotFound() {
            // given
            Long invalidId = 999L;
            given(partyRepository.findById(invalidId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> partyQueryService.getJoinRequests(invalidId, 1L, "PENDING", PageRequest.of(0, 10)))
                    .isInstanceOf(PartyException.class)
                    .satisfies(e -> assertThat(((PartyException) e).getCode())
                            .isEqualTo(PartyErrorCode.PARTY_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("getRecommendedMembers")
    class GetRecommendedMembers {

        @Test
        @DisplayName("성공 - 조건에 맞는 추천 멤버 목록을 정상적으로 조회한다")
        void success_getRecommendedMembers() {
            // given
            Long partyId = 1L;
            String levelSearch = "B";
            Pageable pageable = PageRequest.of(0, 10);

            PartyAddr addr = PartyFixture.createPartyAddr("서울", "강남");
            Party party = PartyFixture.createParty("모임명", 1L, addr);
            ReflectionTestUtils.setField(party, "id", partyId);

            Member suggestedMember = MemberFixture.createMember("추천회원", Gender.MALE, Level.B, 20L);
            ReflectionTestUtils.setField(suggestedMember, "id", 20L);

            Slice<Member> membersSlice = new SliceImpl<>(List.of(suggestedMember), pageable, false);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(memberRepository.findRecommendedMembers(party, levelSearch, pageable))
                    .willReturn(membersSlice);

            // when
            Slice<PartyMemberSuggestionDTO.Response> result = partyQueryService.getRecommendedMembers(partyId, levelSearch, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).userId()).isEqualTo(20L);
            verify(memberRepository).findRecommendedMembers(party, levelSearch, pageable);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 모임의 추천 멤버를 조회하면 PARTY_NOT_FOUND 발생")
        void fail_getRecommendedMembers_partyNotFound() {
            // given
            Long partyId = 999L;
            String levelSearch = null;
            Pageable pageable = PageRequest.of(0, 10);

            given(partyRepository.findById(partyId)).willReturn(Optional.empty());

            // when & then
            PartyException exception = assertThrows(PartyException.class,
                    () -> partyQueryService.getRecommendedMembers(partyId, levelSearch, pageable));
            assertThat(exception.getCode()).isEqualTo(PartyErrorCode.PARTY_NOT_FOUND);
        }
    }
}
