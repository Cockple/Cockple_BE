package umc.cockple.demo.domain.party.service.support;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.exception.ExerciseException;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.repository.PartyRepository;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PartyLookupService {

    private final PartyRepository partyRepository;

    public Party findByIdWithLevelsOrThrow(Long partyId) {
        return partyRepository.findByIdWithLevels(partyId)
                .orElseThrow(() -> new ExerciseException(ExerciseErrorCode.PARTY_NOT_FOUND));
    }
}
