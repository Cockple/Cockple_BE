package umc.cockple.demo.domain.party.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.domain.PartyInvitation;
import umc.cockple.demo.domain.party.enums.RequestStatus;

public interface PartyInvitationRepository extends JpaRepository<PartyInvitation, Long> {
    boolean existsByPartyAndInviteeAndStatus(Party party, Member invitee, RequestStatus requestStatus);

    @EntityGraph(attributePaths = {"party", "inviter"})
    Slice<PartyInvitation> findByInvitee_IdAndStatusOrderByCreatedAtDesc(
            Long inviteeId,
            RequestStatus status,
            Pageable pageable
    );
}
