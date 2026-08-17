package umc.cockple.demo.domain.exercise.service.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;

@Service
@Transactional
@RequiredArgsConstructor
public class ExerciseGameHostRecoveryService {

    private final ExerciseRepository exerciseRepository;

    public int recoverAfterPartyMemberLeft(Long partyId, Long memberId) {
        return exerciseRepository.restoreGameHostToPartyOwner(partyId, memberId);
    }

    public int recoverAfterMemberWithdrawn(Long memberId) {
        return exerciseRepository.restoreGameHostsToPartyOwners(memberId);
    }
}
