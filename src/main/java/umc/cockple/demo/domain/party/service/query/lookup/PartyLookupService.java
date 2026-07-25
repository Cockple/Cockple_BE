package umc.cockple.demo.domain.party.service.query.lookup;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.exception.PartyErrorCode;
import umc.cockple.demo.domain.party.exception.PartyException;
import umc.cockple.demo.domain.party.repository.PartyRepository;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PartyLookupService {

    private final PartyRepository partyRepository;

    public Party findByIdOrThrow(Long partyId) {
        return partyRepository.findById(partyId)
                .orElseThrow(() -> new PartyException(PartyErrorCode.PARTY_NOT_FOUND));
    }

    public Party findByIdWithLevelsOrThrow(Long partyId) {
        return partyRepository.findByIdWithLevels(partyId)
                .orElseThrow(() -> new PartyException(PartyErrorCode.PARTY_NOT_FOUND));
    }
}
