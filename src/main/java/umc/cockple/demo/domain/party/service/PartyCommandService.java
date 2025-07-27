package umc.cockple.demo.domain.party.service;

import umc.cockple.demo.domain.party.dto.PartyCreateDTO;
import umc.cockple.demo.domain.party.dto.PartyJoinActionDTO;
import umc.cockple.demo.domain.party.dto.PartyJoinCreateDTO;

public interface PartyCommandService {

    PartyCreateDTO.Response createParty(Long memberId, PartyCreateDTO.Request request);

    PartyJoinCreateDTO.Response createJoinRequest(Long partyId, Long memberId);

    void actionJoinRequest(Long partyId, Long memberId, PartyJoinActionDTO.Request request, Long requestId);

    void deleteParty(Long partyId, Long memberId);

    void leaveParty(Long partyId, Long memberId);

    void removeMember(Long partyId, Long memberIdToRemove, Long currentMemberId);
}
