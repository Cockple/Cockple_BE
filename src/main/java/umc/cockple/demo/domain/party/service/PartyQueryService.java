package umc.cockple.demo.domain.party.service;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import umc.cockple.demo.domain.party.dto.PartyJoinResponseDTO;

public interface PartyQueryService {
    Slice<PartyJoinResponseDTO> getJoinRequests(Long partyId, Long memberId, Pageable pageable);
}
