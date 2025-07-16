package umc.cockple.demo.domain.party.service;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface PartyQueryService {
    Slice<PartyJoinResponseDTO> getJoinRequests(Long partyId, Long memberId, Pageable pageable);
}
