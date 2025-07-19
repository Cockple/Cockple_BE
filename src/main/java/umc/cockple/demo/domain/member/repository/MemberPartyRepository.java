package umc.cockple.demo.domain.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberParty;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.global.enums.Role;

import java.util.List;

public interface MemberPartyRepository extends JpaRepository<MemberParty, Long> {

    boolean existsByPartyIdAndMemberIdAndRole(Long partyId, Long memberId, Role role);

    boolean existsByPartyAndMember(Party party, Member member);

    @Query("""
            SELECT mp FROM MemberParty mp
            WHERE mp.party.id = :partyId
            AND mp.member.id IN :memberIds
            """)
    List<MemberParty> findMemberRolesByPartyAndMembers(
            @Param("partyId") Long partyId, @Param("memberIds") List<Long> memberIds);

}
