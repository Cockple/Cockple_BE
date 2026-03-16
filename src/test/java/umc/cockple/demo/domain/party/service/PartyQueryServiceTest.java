package umc.cockple.demo.domain.party.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberParty;
import umc.cockple.demo.domain.member.repository.MemberExerciseRepository;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.party.converter.PartyConverter;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.domain.PartyAddr;
import umc.cockple.demo.domain.party.dto.*;
import umc.cockple.demo.domain.party.enums.RequestStatus;
import umc.cockple.demo.domain.party.exception.PartyErrorCode;
import umc.cockple.demo.domain.party.exception.PartyException;
import umc.cockple.demo.domain.party.repository.PartyRepository;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.global.enums.Role;
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import umc.cockple.demo.domain.bookmark.repository.PartyBookmarkRepository;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.member.repository.MemberAddrRepository;
import umc.cockple.demo.domain.file.service.FileService;
import umc.cockple.demo.domain.party.repository.PartyJoinRequestRepository;
import umc.cockple.demo.domain.member.domain.MemberAddr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    @Mock
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

    @Nested
    @DisplayName("getPartyMembers")
    class GetPartyMembers {

        @Test
        @DisplayName("모임의 멤버들을 역할별로 성공적으로 조회한다.")
        void success() {
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

            MemberParty mp1 = MemberFixture.createMemberParty(party, manager, Role.party_MANAGER);
            MemberParty mp2 = MemberFixture.createMemberParty(party, subManager, Role.party_SUBMANAGER);
            MemberParty mp3 = MemberFixture.createMemberParty(party, normalMember, Role.party_MEMBER);
            List<MemberParty> memberParties = List.of(mp1, mp2, mp3);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(memberPartyRepository.findAllByPartyIdWithMember(partyId)).willReturn(memberParties);
            given(memberExerciseRepository.findLastExerciseDateByMemberIdsAndPartyId(anyList(),
                    eq(partyId)))
                    .willReturn(List.of());

            // when
            partyQueryService.getPartyMembers(partyId, currentMemberId);

            // then
            verify(partyConverter).toPartyMemberDTO(eq(memberParties), eq(currentMemberId), anyMap());
        }

        @Test
        @DisplayName("멤버 목록과 마지막 운동일을 함께 반환한다")
        void success_withExerciseHistory() {
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

            MemberParty mp1 = MemberFixture.createMemberParty(party, manager, Role.party_MANAGER);
            MemberParty mp2 = MemberFixture.createMemberParty(party, member1, Role.party_MEMBER);
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
            given(partyConverter.toPartyMemberDTO(eq(memberParties), eq(currentMemberId), any()))
                    .willReturn(expected);

            // when
            PartyMemberDTO.Response result = partyQueryService.getPartyMembers(partyId, currentMemberId);

            // then
            assertThat(result).isEqualTo(expected);
            verify(memberExerciseRepository).findLastExerciseDateByMemberIdsAndPartyId(
                    List.of(10L, 20L), partyId);
            verify(partyConverter).toPartyMemberDTO(
                    eq(memberParties),
                    eq(currentMemberId),
                    eq(Map.of(20L, lastDate)));
        }

        @Test
        @DisplayName("운동 기록이 없는 멤버는 빈 Map이 converter에 전달된다")
        void noExerciseHistory() {
            // given
            Long partyId = 1L;
            Long currentMemberId = 10L;

            PartyAddr addr = PartyFixture.createPartyAddr("서울특별시", "강남구");
            Party party = PartyFixture.createParty("테스트 모임", 10L, addr);
            ReflectionTestUtils.setField(party, "id", partyId);
            Member manager = MemberFixture.createMember("매니저", Gender.MALE, Level.A, 1001L);
            ReflectionTestUtils.setField(manager, "id", 10L);
            MemberParty mp = MemberFixture.createMemberParty(party, manager, Role.party_MANAGER);
            List<MemberParty> memberParties = List.of(mp);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(memberPartyRepository.findAllByPartyIdWithMember(partyId)).willReturn(memberParties);
            given(memberExerciseRepository.findLastExerciseDateByMemberIdsAndPartyId(
                    List.of(10L), partyId)).willReturn(List.of());
            given(partyConverter.toPartyMemberDTO(any(), any(), any())).willReturn(null);

            // when
            partyQueryService.getPartyMembers(partyId, currentMemberId);

            // then
            verify(partyConverter).toPartyMemberDTO(
                    eq(memberParties),
                    eq(currentMemberId),
                    eq(Map.of()));
        }

        @Test
        @DisplayName("존재하지 않는 파티면 PartyException을 던진다")
        void partyNotFound() {
            // given
            given(partyRepository.findById(99L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> partyQueryService.getPartyMembers(99L, 1L))
                    .isInstanceOf(PartyException.class)
                    .satisfies(e -> assertThat(((PartyException) e).getCode())
                            .isEqualTo(PartyErrorCode.PARTY_NOT_FOUND));
        }

        @Test
        @DisplayName("비활성화된 파티면 PartyException을 던진다")
        void partyInactive() {
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
        @DisplayName("내 모임 목록과 부가 정보(운동 횟수, 다음 운동 정보, 북마크 여부)를 조합하여 반환한다")
        void success() {
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
            given(partyConverter.toMyPartyDTO(eq(party), any(), any(), any(), eq(true)))
                    .willReturn(expectedResponse);

            // when
            Slice<PartyDTO.Response> result = partyQueryService.getMyParties(memberId, false, "최신순",
                    pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).partyName()).isEqualTo("테스트 모임");
            assertThat(result.getContent().get(0).isBookmarked()).isTrue();

            verify(partyRepository).findMyParty(eq(memberId), eq(false), any(Pageable.class));
            verify(partyConverter).toMyPartyDTO(eq(party), any(), any(), any(), eq(true));
        }
    }

    @Nested
    @DisplayName("getSimpleMyParties")
    class GetSimpleMyParties {

        @Test
        @DisplayName("유효한 회원 ID가 주어지면 가입한 모임의 간략화된 목록을 반환한다")
        void success() {
            // given
            Long memberId = 1L;
            Pageable pageable = PageRequest.of(0, 10);

            Member member = MemberFixture.createMember("사용자", Gender.MALE, Level.A, 1001L);
            ReflectionTestUtils.setField(member, "id", memberId);

            PartyAddr addr = PartyFixture.createPartyAddr("서울특별시", "강남구");
            Party party = PartyFixture.createParty("테스트 모임", 10L, addr);
            ReflectionTestUtils.setField(party, "id", 10L);

            MemberParty memberParty = MemberFixture.createMemberParty(party, member, Role.party_MEMBER);

            Slice<MemberParty> memberPartySlice = new SliceImpl<>(List.of(memberParty), pageable, false);

            PartySimpleDTO.Response expectedResponse = PartySimpleDTO.Response.builder()
                    .partyId(10L)
                    .partyName("테스트 모임")
                    .build();

            given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
            given(memberPartyRepository.findByMember(member, pageable)).willReturn(memberPartySlice);
            // fileService.getUrlFromKey 의 경우 partyImg 가 없으므로 널이 전달됨
            given(partyConverter.toPartySimpleDTO(eq(memberParty), any())).willReturn(expectedResponse);

            // when
            Slice<PartySimpleDTO.Response> result = partyQueryService.getSimpleMyParties(memberId,
                    pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).partyName()).isEqualTo("테스트 모임");

            verify(memberRepository).findById(memberId);
            verify(memberPartyRepository).findByMember(member, pageable);
            verify(partyConverter).toPartySimpleDTO(eq(memberParty), any());
        }

        @Test
        @DisplayName("존재하지 않는 회원일 경우 MemberException을 던진다")
        void memberNotFound() {
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
        @DisplayName("Cockple 추천 모드 시 유저 정보(주소, 생년월일, 키워드)를 기반으로 추천 목록을 반환한다")
        void success_cockpleRecommend() {
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
            given(partyConverter.toMyPartyDTO(eq(suggestedParty), any(), any(), any(), eq(false)))
                    .willReturn(PartyDTO.Response.builder().partyId(100L).partyName("추천 모임")
                            .build());

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
        @DisplayName("필터 모드 시 설정한 필터 조건(addr1, addr2 등)에 맞는 모임 목록을 반환한다")
        void success_filterMode() {
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
            given(partyConverter.toMyPartyDTO(eq(filteredParty), any(), any(), any(), eq(false)))
                    .willReturn(PartyDTO.Response.builder().partyId(200L).partyName("필터 모임")
                            .build());

            // when
            Slice<PartyDTO.Response> result = partyQueryService.getRecommendedParties(memberId, false,
                    filter, "최신순", pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).partyName()).isEqualTo("필터 모임");
            verify(partyRepository).searchParties(eq(memberId), eq(filter), any(Pageable.class));
        }

        @Test
        @DisplayName("존재하지 않는 회원 ID로 추천 요청 시 MEMBER_NOT_FOUND이 발생한다")
        void fail_memberNotFound() {
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
        @DisplayName("대표 주소가 설정되지 않은 회원이 추천 요청 시 MAIN_ADDRESS_NULL이 발생한다")
        void fail_mainAddressNotFound() {
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
        @DisplayName("모임 상세 정보를 정상적으로 조회한다 (비회원, 신청 전)")
        void success_nonMember() {
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
            given(partyConverter.toPartyDetailResponseDTO(eq(party), any(), any(), eq(false), eq(false)))
                    .willReturn(expected);

            // when
            PartyDetailDTO.Response result = partyQueryService.getPartyDetails(partyId, memberId);

            // then
            assertThat(result).isEqualTo(expected);
            verify(partyRepository).findById(partyId);
        }

        @Test
        @DisplayName("모임원인 경우 memberStatus가 MEMBER로 반환된다")
        void success_member() {
            // given
            Long partyId = 1L;
            Long memberId = 10L;

            PartyAddr addr = PartyFixture.createPartyAddr("서울", "강남");
            Party party = PartyFixture.createParty("상세 모임", 11L, addr);
            ReflectionTestUtils.setField(party, "id", partyId);
            Member member = MemberFixture.createMember("사용자", Gender.MALE, Level.A, 1000L);
            ReflectionTestUtils.setField(member, "id", memberId);

            MemberParty memberParty = MemberFixture.createMemberParty(party, member, Role.party_MEMBER);
            PartyDetailDTO.Response expected = PartyDetailDTO.Response.builder()
                    .partyId(partyId)
                    .memberStatus("MEMBER")
                    .memberRole("party_MEMBER")
                    .build();

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
            given(memberPartyRepository.findByPartyAndMember(party, member))
                    .willReturn(Optional.of(memberParty));
            given(partyConverter.toPartyDetailResponseDTO(eq(party), any(), any(), eq(false), anyBoolean()))
                    .willReturn(expected);

            // when
            PartyDetailDTO.Response result = partyQueryService.getPartyDetails(partyId, memberId);

            // then
            assertThat(result.memberStatus()).isEqualTo("MEMBER");
        }

        @Test
        @DisplayName("존재하지 않는 모임 조회 시 PARTY_NOT_FOUND이 발생한다")
        void fail_partyNotFound() {
            // given
            given(partyRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> partyQueryService.getPartyDetails(999L, 1L))
                    .isInstanceOf(PartyException.class)
                    .satisfies(e -> assertThat(((PartyException) e).getCode())
                            .isEqualTo(PartyErrorCode.PARTY_NOT_FOUND));
        }

        @Test
        @DisplayName("삭제된 모임 조회 시 PARTY_IS_DELETED이 발생한다")
        void fail_partyDeleted() {
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
    }
}
