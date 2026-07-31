package umc.cockple.demo.domain.party.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.party.domain.PartyInvitation;
import umc.cockple.demo.domain.party.dto.PartyInvitationResponseDTO;
import umc.cockple.demo.domain.party.enums.RequestStatus;
import umc.cockple.demo.domain.party.repository.PartyInvitationRepository;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PartyInvitationQueryService {

    private final PartyInvitationRepository partyInvitationRepository;

    public Slice<PartyInvitationResponseDTO> getReceivedInvitations(
            Long memberId,
            RequestStatus status,
            Pageable pageable
    ) {
        return partyInvitationRepository
                .findByInvitee_IdAndStatusOrderByCreatedAtDesc(memberId, status, pageable)
                .map(this::toResponse);
    }

    private PartyInvitationResponseDTO toResponse(PartyInvitation invitation) {
        return PartyInvitationResponseDTO.builder()
                .invitationId(invitation.getId())
                .partyId(invitation.getParty().getId())
                .partyName(invitation.getParty().getPartyName())
                .inviterId(invitation.getInviter().getId())
                .inviterNickname(invitation.getInviter().getNickname())
                .status(invitation.getStatus())
                .createdAt(invitation.getCreatedAt())
                .build();
    }
}
