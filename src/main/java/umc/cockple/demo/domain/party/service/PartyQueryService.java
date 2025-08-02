package umc.cockple.demo.domain.party.service;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import umc.cockple.demo.domain.party.dto.*;

public interface PartyQueryService {
    Slice<PartyJoinDTO.Response> getJoinRequests(Long partyId, Long memberId, String status, Pageable pageable);
    PartyDetailDTO.Response getPartyDetails(Long partyId, Long memberId);
    Slice<PartySimpleDTO.Response> getSimpleMyParties(Long memberId, Pageable pageable);
    Slice<PartyDTO.Response> getMyParties(Long memberId, Boolean created, String sort, Pageable pageable);
    PartyMemberDTO.Response getPartyMembers(Long partyId, Long currentMemberId);
    Slice<PartyDTO.Response> getRecommendedParties(Long memberId, Boolean isCockpleRecommend, PartyFilterDTO.Request filter, String sort, Pageable pageable);
    Slice<PartyMemberSuggestionDTO.Response> getRecommendedMembers(Long partyId, String levelSearch, Pageable pageable);
}
