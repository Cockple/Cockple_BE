package umc.cockple.demo.domain.game.service.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 시작 후 일정 시간이 지난 진행 게임을 주기적으로 스캔해 자동 완료 처리
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!integrationtest")
public class GameAutoCompleteScheduler {

    private static final long SCAN_INTERVAL_MS = 60_000L;

    private final GameAutoCompleteService gameAutoCompleteService;

    @Scheduled(initialDelay = SCAN_INTERVAL_MS, fixedDelay = SCAN_INTERVAL_MS)
    public void run() {
        try {
            gameAutoCompleteService.autoCompleteStaleGames();
        } catch (Exception e) {
            log.error("게임 자동 완료 스케줄러 실행 실패", e);
        }
    }
}
