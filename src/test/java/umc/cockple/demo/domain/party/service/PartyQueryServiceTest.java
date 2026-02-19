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
import umc.cockple.demo.domain.party.converter.PartyConverter;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.domain.PartyAddr;
import umc.cockple.demo.domain.party.dto.PartyMemberDTO;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PartyQueryServiceTest {

    @InjectMocks
    private PartyQueryServiceImpl partyQueryService;

    @Mock
    private PartyRepository partyRepository;
    @Mock
    private PartyConverter partyConverter;
    @Mock
    private MemberPartyRepository memberPartyRepository;
    @Mock
    private MemberExerciseRepository memberExerciseRepository;

    @Nested
    @DisplayName("getPartyMembers")
    class GetPartyMembers {

        @Test
        @DisplayName("멤버 목록과 마지막 운동일을 함께 반환한다")
        void success() {
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
                    eq(Map.of(20L, lastDate))
            );
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
                    eq(Map.of())
            );
        }

        @Test
        @DisplayName("존재하지 않는 파티면 PartyException을 던진다")
        void partyNotFound() {
            // given
            given(partyRepository.findById(99L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> partyQueryService.getPartyMembers(99L, 1L))
                    .isInstanceOf(PartyException.class)
                    .satisfies(e -> assertThat(((PartyException) e).getCode()).isEqualTo(PartyErrorCode.PARTY_NOT_FOUND));
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
                    .satisfies(e -> assertThat(((PartyException) e).getCode()).isEqualTo(PartyErrorCode.PARTY_IS_DELETED));
        }
    }

}
