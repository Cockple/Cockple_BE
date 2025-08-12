package umc.cockple.demo.domain.party.service;

import umc.cockple.demo.domain.party.dto.*;

public interface PartyCommandService {
    PartyCreateDTO.Response createParty(Long memberId, PartyCreateDTO.Request request);
    PartyJoinCreateDTO.Response createJoinRequest(Long partyId, Long memberId);
    void actionJoinRequest(Long partyId, Long memberId, PartyJoinActionDTO.Request request, Long requestId);
    void deleteParty(Long partyId, Long memberId);
    void leaveParty(Long partyId, Long memberId);
    void removeMember(Long partyId, Long memberIdToRemove, Long currentMemberId);
    PartyInviteCreateDTO.Response createInvitation(Long partyId, Long memberIdToInvite, Long currentMemberId);
    void actionInvitation(Long memberId, PartyInviteActionDTO.Request request, Long invitationId);
    void updateParty(Long partyId, Long memberId, PartyUpdateDTO.Request request);
    void addKeyword(Long partyId, Long memberID, PartyKeywordDTO.Request request);
}
