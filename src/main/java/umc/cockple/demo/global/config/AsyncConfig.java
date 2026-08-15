package umc.cockple.demo.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import umc.cockple.demo.global.logging.MdcTaskDecorator;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 지연 특성별로 벌크헤드(bulkhead) 격리
 * {@code chatExecutor} — 실시간/저지연
 * {@code notificationPushExecutor} — 외부 FCM HTTP 호출
 * {@code applicationTaskExecutor} — qualifier 없는 @Async의 기본 executor
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig {

    /** Spring이 qualifier 없는 @Async의 기본 executor를 찾는 이름. */
    private static final String DEFAULT_TASK_EXECUTOR_BEAN_NAME = "taskExecutor";

    @Bean
    public TaskDecorator mdcTaskDecorator() {
        return new MdcTaskDecorator();
    }

    @Bean(name = {
            TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME,
            DEFAULT_TASK_EXECUTOR_BEAN_NAME
    })
    public ThreadPoolTaskExecutor applicationTaskExecutor(
            ThreadPoolTaskExecutorBuilder builder,
            TaskDecorator mdcTaskDecorator
    ) {
        return builder
                .threadNamePrefix("cockple-async-")
                .taskDecorator(mdcTaskDecorator)
                .build();
    }

    /**
     * 실시간 채팅용 풀-> 짧은 내부 I/O 작업을 저지연으로 처리
     */
    @Bean("chatExecutor")
    public ThreadPoolTaskExecutor chatExecutor(
            @Value("${async.chat.core-size:8}") int coreSize,
            @Value("${async.chat.max-size:16}") int maxSize,
            @Value("${async.chat.queue-capacity:500}") int queueCapacity,
            @Value("${async.chat.await-termination-seconds:30}") int awaitTerminationSeconds,
            TaskDecorator mdcTaskDecorator
    ) {
        // 채팅은 유실 방지가 우선 -> 포화 시 호출 스레드에서 실행해 백프레셔를 건다.
        return buildExecutor("cockple-chat-", coreSize, maxSize, queueCapacity,
                awaitTerminationSeconds, mdcTaskDecorator, new ThreadPoolExecutor.CallerRunsPolicy());
    }

    /**
     * FCM 알림용 풀. 외부 Firebase HTTP 호출을 알림 DB 저장과 격리한다.
     * 큐를 크게 잡되 유한하게 두어 Firebase 장애 시 무한 백로그(OOM)를 방지한다.
     */
    @Bean("notificationPushExecutor")
    public ThreadPoolTaskExecutor notificationPushExecutor(
            @Value("${async.notification.push.core-size:2}") int coreSize,
            @Value("${async.notification.push.max-size:8}") int maxSize,
            @Value("${async.notification.push.queue-capacity:500}") int queueCapacity,
            @Value("${async.notification.push.await-termination-seconds:30}") int awaitTerminationSeconds,
            TaskDecorator mdcTaskDecorator
    ) {
        return buildExecutor("cockple-noti-push-", coreSize, maxSize, queueCapacity,
                awaitTerminationSeconds, mdcTaskDecorator, logAndDiscardPolicy());
    }

    /**
     * 큐/스레드 포화로 거부된 작업을 예외 전파 없이 버림
     */
    private RejectedExecutionHandler logAndDiscardPolicy() {
        // TODO: 알림 재전송 도입 시, 여기서 drop 대신 outbox/DLQ에 적재해 재시도 훅으로 사용
        return (runnable, executor) ->
                log.warn("[NOTIFICATION] 알림 풀 포화로 작업 drop - poolSize={}, activeCount={}, queueSize={}",
                        executor.getPoolSize(), executor.getActiveCount(), executor.getQueue().size());
    }

    private ThreadPoolTaskExecutor buildExecutor(String threadNamePrefix, int coreSize, int maxSize,
                                                 int queueCapacity, int awaitTerminationSeconds,
                                                 TaskDecorator mdcTaskDecorator,
                                                 RejectedExecutionHandler rejectedExecutionHandler) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(coreSize);
        executor.setMaxPoolSize(maxSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setTaskDecorator(mdcTaskDecorator);
        executor.setRejectedExecutionHandler(rejectedExecutionHandler);
        // graceful shutdown: 진행 중인 작업이 끝날 때까지 대기
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(awaitTerminationSeconds);
        // 스프링이 빈 초기화 시 afterPropertiesSet()으로 initialize()를 호출한다.
        return executor;
    }
}
