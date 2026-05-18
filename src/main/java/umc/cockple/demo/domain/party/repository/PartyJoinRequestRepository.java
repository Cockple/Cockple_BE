package umc.cockple.demo.domain.party.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.domain.PartyJoinRequest;
import umc.cockple.demo.domain.party.enums.RequestStatus;

import java.util.List;

public interface PartyJoinRequestRepository extends JpaRepository<PartyJoinRequest, Long> {
    boolean existsByPartyAndMemberAndStatus(Party party, Member member, RequestStatus requestStatus);
    Slice<PartyJoinRequest> findByPartyAndStatus(Party party, RequestStatus status, Pageable pageable);

    @Modifying
    @Query("DELETE FROM PartyJoinRequest pjr WHERE pjr.member.id IN :memberIds")
    void deleteByMemberIds(@Param("memberIds") List<Long> memberIds);
}
