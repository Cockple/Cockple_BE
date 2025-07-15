package umc.cockple.demo.domain.party.service;

import org.springframework.web.multipart.MultipartFile;
import umc.cockple.demo.domain.party.dto.PartyCreateRequestDTO;
import umc.cockple.demo.domain.party.dto.PartyCreateResponseDTO;
import umc.cockple.demo.domain.party.dto.PartyJoinActionRequestDTO;
import umc.cockple.demo.domain.party.dto.PartyJoinCreateResponseDTO;
import umc.cockple.demo.global.response.BaseResponse;

public interface PartyCommandService {

    PartyCreateResponseDTO createParty(Long memberId, MultipartFile profileImage, PartyCreateRequestDTO request);

    PartyJoinCreateResponseDTO createJoinRequest(Long partyId, Long memberId);

    void actionJoinRequest(Long partyId, Long memberId, PartyJoinActionRequestDTO request, Long requestId);
}
