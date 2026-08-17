package umc.cockple.demo.domain.game.service.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.game.repository.GameBoardMemberRepository;
import umc.cockple.demo.domain.game.repository.GamePlayerRepository;

import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class GameBoardRosterCleanupService {

    private final GameBoardMemberRepository gameBoardMemberRepository;
    private final GamePlayerRepository gamePlayerRepository;

    public boolean hasActiveFutureAssignment(Long memberId, LocalDateTime referenceTime) {
        return gamePlayerRepository.countActiveFutureAssignmentsByMemberId(
                memberId, referenceTime.toLocalDate(), referenceTime.toLocalTime()) > 0;
    }

    public int removeFutureMemberRosters(Long memberId, LocalDateTime referenceTime) {
        int removedCount = gameBoardMemberRepository.deleteFutureByMemberId(
                memberId, referenceTime.toLocalDate(), referenceTime.toLocalTime());

        log.info("탈퇴 회원의 미래 게임판 명단 정리 완료 - memberId: {}, count: {}",
                memberId, removedCount);
        return removedCount;
    }
}
