package umc.cockple.demo.domain.party.service;

public interface PartyCommandService {

    PartyCreateResponseDTO createParty(Long memberId, PartyCreateRequestDTO request);

    PartyJoinCreateResponseDTO createJoinRequest(Long partyId, Long memberId);

    void actionJoinRequest(Long partyId, Long memberId, PartyJoinActionRequestDTO request, Long requestId);
}
