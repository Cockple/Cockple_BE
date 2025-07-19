package umc.cockple.demo.domain.party.service;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import umc.cockple.demo.domain.party.dto.PartyDetailDTO;
import umc.cockple.demo.domain.party.dto.PartyJoinDTO;

public interface PartyQueryService {
    Slice<PartyJoinDTO.Response> getJoinRequests(Long partyId, Long memberId, String status, Pageable pageable);
    PartyDetailDTO.Response getPartyDetails(Long partyId, Long memberId);
}
