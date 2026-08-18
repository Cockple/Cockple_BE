package umc.cockple.demo.domain.member.service.query.lookup;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberParty;
import umc.cockple.demo.domain.member.enums.MemberPartyStatus;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.global.enums.Role;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberPartyLookupService {

    private final MemberPartyRepository memberPartyRepository;

    public boolean isPartyMember(Party party, Member member) {
        return memberPartyRepository.existsByPartyAndMember(party, member);
    }

    public boolean hasRole(Party party, Member member, Role role) {
        return hasRole(party.getId(), member.getId(), role);
    }

    public boolean hasRole(Long partyId, Long memberId, Role role) {
        return memberPartyRepository.existsByPartyIdAndMemberIdAndRole(partyId, memberId, role);
    }

    public boolean hasAnyActiveRole(Long partyId, Long memberId, Collection<Role> roles) {
        return memberPartyRepository.existsByPartyIdAndMemberIdAndStatusAndRoleIn(
                partyId, memberId, MemberPartyStatus.ACTIVE, roles);
    }

    public List<Long> findPartyIdsByMemberId(Long memberId) {
        return memberPartyRepository.findPartyIdsByMemberId(memberId);
    }

    @Transactional
    public Optional<MemberParty> findActiveMemberForUpdate(Long partyId, Long memberId) {
        return memberPartyRepository.findByPartyIdAndMemberIdAndStatusForUpdate(
                partyId, memberId, MemberPartyStatus.ACTIVE);
    }

    public List<MemberParty> findActiveMembersWithProfile(Long partyId) {
        return memberPartyRepository.findAllByPartyIdAndStatusWithMemberAndProfile(
                partyId, MemberPartyStatus.ACTIVE);
    }

    public Map<Long, Role> findMemberRolesByPartyAndMembers(Long partyId, List<Long> memberIds) {
        return memberPartyRepository.findMemberRolesByPartyAndMembers(partyId, memberIds)
                .stream()
                .collect(Collectors.toMap(
                        memberParty -> memberParty.getMember().getId(),
                        MemberParty::getRole
                ));
    }
}
