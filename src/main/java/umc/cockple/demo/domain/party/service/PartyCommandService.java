package umc.cockple.demo.domain.party.service;

import org.springframework.web.multipart.MultipartFile;
import umc.cockple.demo.domain.party.dto.PartyCreateRequestDTO;
import umc.cockple.demo.domain.party.dto.PartyCreateResponseDTO;
import umc.cockple.demo.domain.party.dto.PartyJoinResponseDTO;

public interface PartyCommandService {

    PartyCreateResponseDTO createParty(Long memberId, MultipartFile profileImage, PartyCreateRequestDTO request);

    PartyJoinResponseDTO createJoinRequest(Long partyId, Long memberId);
}
