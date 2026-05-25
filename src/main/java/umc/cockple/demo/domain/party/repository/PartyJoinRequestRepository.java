package umc.cockple.demo.domain.party.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.domain.PartyJoinRequest;
import umc.cockple.demo.domain.party.enums.RequestStatus;

public interface PartyJoinRequestRepository extends JpaRepository<PartyJoinRequest, Long> {
    boolean existsByPartyAndMemberAndStatus(Party party, Member member, RequestStatus requestStatus);
    Slice<PartyJoinRequest> findByPartyAndStatus(Party party, RequestStatus status, Pageable pageable);
}
