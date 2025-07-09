package umc.cockple.demo.domain.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberParty;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.global.enums.Role;

public interface MemberPartyRepository extends JpaRepository<MemberParty, Long> {

    boolean existsByPartyIdAndMemberIdAndRole(Long partyId, Long memberId, Role role);

    boolean existsByPartyAndMember(Party party, Member member);
}
