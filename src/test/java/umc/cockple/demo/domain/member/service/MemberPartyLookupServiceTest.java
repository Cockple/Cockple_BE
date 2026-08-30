package umc.cockple.demo.domain.member.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberParty;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.member.service.query.lookup.MemberPartyLookupService;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.global.enums.Role;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberPartyLookupService")
class MemberPartyLookupServiceTest {

    @InjectMocks
    private MemberPartyLookupService memberPartyLookupService;

    @Mock private MemberPartyRepository memberPartyRepository;
    @Mock private Party party;
    @Mock private Member member;
    @Mock private MemberParty memberParty;

    @Test
    @DisplayName("모임과 멤버로 가입 여부를 조회한다")
    void isPartyMember_returnsMembershipStatus() {
        given(memberPartyRepository.existsByPartyAndMember(party, member)).willReturn(true);

        assertThat(memberPartyLookupService.isPartyMember(party, member)).isTrue();
    }

    @Test
    @DisplayName("모임과 멤버의 역할 보유 여부를 조회한다")
    void hasRole_withEntities_returnsRoleStatus() {
        given(party.getId()).willReturn(1L);
        given(member.getId()).willReturn(2L);
        given(memberPartyRepository.existsByPartyIdAndMemberIdAndRole(1L, 2L, Role.PARTY_MANAGER))
                .willReturn(true);

        assertThat(memberPartyLookupService.hasRole(party, member, Role.PARTY_MANAGER)).isTrue();
    }

    @Test
    @DisplayName("멤버가 가입한 모임 ID를 조회한다")
    void findPartyIdsByMemberId_returnsPartyIds() {
        given(memberPartyRepository.findPartyIdsByMemberId(2L)).willReturn(List.of(1L, 3L));

        assertThat(memberPartyLookupService.findPartyIdsByMemberId(2L))
                .containsExactly(1L, 3L);
    }

    @Test
    @DisplayName("모임 멤버별 역할을 조회한다")
    void findMemberRolesByPartyAndMembers_returnsRoleMap() {
        given(memberPartyRepository.findMemberRolesByPartyAndMembers(1L, List.of(2L)))
                .willReturn(List.of(memberParty));
        given(memberParty.getMember()).willReturn(member);
        given(member.getId()).willReturn(2L);
        given(memberParty.getRole()).willReturn(Role.PARTY_SUBMANAGER);

        Map<Long, Role> roles = memberPartyLookupService
                .findMemberRolesByPartyAndMembers(1L, List.of(2L));

        assertThat(roles).containsEntry(2L, Role.PARTY_SUBMANAGER);
    }
}
