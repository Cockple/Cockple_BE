package umc.cockple.demo.global.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import umc.cockple.demo.global.security.filter.JwtAuthenticationFilter;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class MdcTaskDecoratorTest {

    private final MdcTaskDecorator decorator = new MdcTaskDecorator();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("decorate 시점의 MDC를 작업 실행 동안 전파하고 이전 MDC를 복구한다")
    void copyParentMdcAndRestorePreviousMdc() {
        MDC.put(MdcLoggingFilter.REQUEST_ID, "request-1");
        MDC.put(JwtAuthenticationFilter.MEMBER_ID, "7");

        AtomicReference<String> requestIdInTask = new AtomicReference<>();
        AtomicReference<String> memberIdInTask = new AtomicReference<>();
        Runnable decorated = decorator.decorate(() -> {
            requestIdInTask.set(MDC.get(MdcLoggingFilter.REQUEST_ID));
            memberIdInTask.set(MDC.get(JwtAuthenticationFilter.MEMBER_ID));
        });

        MDC.clear();
        MDC.put(MdcLoggingFilter.REQUEST_ID, "worker-before");

        decorated.run();

        assertThat(requestIdInTask.get()).isEqualTo("request-1");
        assertThat(memberIdInTask.get()).isEqualTo("7");
        assertThat(MDC.get(MdcLoggingFilter.REQUEST_ID)).isEqualTo("worker-before");
        assertThat(MDC.get(JwtAuthenticationFilter.MEMBER_ID)).isNull();
    }

    @Test
    @DisplayName("decorate 시점에 MDC가 없으면 작업 실행 동안 기존 worker MDC를 비운 뒤 복구한다")
    void clearWorkerMdcWhenParentMdcIsEmptyAndRestoreAfterwards() {
        MDC.clear();

        AtomicReference<String> requestIdInTask = new AtomicReference<>();
        Runnable decorated = decorator.decorate(() ->
                requestIdInTask.set(MDC.get(MdcLoggingFilter.REQUEST_ID))
        );

        MDC.put(MdcLoggingFilter.REQUEST_ID, "worker-before");

        decorated.run();

        assertThat(requestIdInTask.get()).isNull();
        assertThat(MDC.get(MdcLoggingFilter.REQUEST_ID)).isEqualTo("worker-before");
    }
}
