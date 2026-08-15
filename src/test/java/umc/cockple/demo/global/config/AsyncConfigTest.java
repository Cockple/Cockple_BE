package umc.cockple.demo.global.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import umc.cockple.demo.global.logging.MdcLoggingFilter;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncConfigTest {

    private final AsyncConfig config = new AsyncConfig();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("applicationTaskExecutor는 MDC를 비동기 작업에 전파하고 다음 작업으로 누수하지 않는다")
    void applicationTaskExecutorPropagatesMdcAndPreventsLeak() throws Exception {
        ThreadPoolTaskExecutor executor = config.applicationTaskExecutor(
                new ThreadPoolTaskExecutorBuilder()
                        .corePoolSize(1)
                        .maxPoolSize(1)
                        .queueCapacity(1),
                config.mdcTaskDecorator()
        );

        assertPropagatesMdcAndPreventsLeak(executor);
    }

    @Test
    @DisplayName("chatExecutor는 MDC를 비동기 작업에 전파하고 다음 작업으로 누수하지 않는다")
    void chatExecutorPropagatesMdcAndPreventsLeak() throws Exception {
        ThreadPoolTaskExecutor executor = config.chatExecutor(1, 1, 1, 1, config.mdcTaskDecorator());

        assertPropagatesMdcAndPreventsLeak(executor);
    }

    @Test
    @DisplayName("notificationPushExecutor는 MDC를 비동기 작업에 전파하고 다음 작업으로 누수하지 않는다")
    void notificationPushExecutorPropagatesMdcAndPreventsLeak() throws Exception {
        ThreadPoolTaskExecutor executor = config.notificationPushExecutor(1, 1, 1, 1, config.mdcTaskDecorator());

        assertPropagatesMdcAndPreventsLeak(executor);
    }

    /**
     * 풀에서 도는 작업에 requestId(MDC)가 전파되고, 스레드 재사용 시 이전 컨텍스트가 누수되지 않는지 검증한다.
     */
    private void assertPropagatesMdcAndPreventsLeak(ThreadPoolTaskExecutor executor) throws Exception {
        executor.initialize();
        try {
            MDC.put(MdcLoggingFilter.REQUEST_ID, "async-request");
            CompletableFuture<String> firstTask = new CompletableFuture<>();
            executor.execute(() -> firstTask.complete(MDC.get(MdcLoggingFilter.REQUEST_ID)));

            assertThat(firstTask.get(3, TimeUnit.SECONDS)).isEqualTo("async-request");

            MDC.clear();
            CompletableFuture<String> secondTask = new CompletableFuture<>();
            executor.execute(() -> secondTask.complete(MDC.get(MdcLoggingFilter.REQUEST_ID)));

            assertThat(secondTask.get(3, TimeUnit.SECONDS)).isNull();
        } finally {
            executor.shutdown();
        }
    }
}
