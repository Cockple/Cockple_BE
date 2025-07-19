package umc.cockple.demo.domain.party.service;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import umc.cockple.demo.domain.party.dto.PartyDTO;
import umc.cockple.demo.domain.party.dto.PartyDetailDTO;
import umc.cockple.demo.domain.party.dto.PartyJoinDTO;
import umc.cockple.demo.domain.party.dto.PartySimpleDTO;

public interface PartyQueryService {
    Slice<PartyJoinDTO.Response> getJoinRequests(Long partyId, Long memberId, String status, Pageable pageable);
    PartyDetailDTO.Response getPartyDetails(Long partyId, Long memberId);
    Slice<PartySimpleDTO.Response> getSimpleMyParties(Long memberId, Pageable pageable);
    Slice<PartyDTO.Response> getMyParties(Long memberId, Boolean created, String sort, Pageable pageable);
}
