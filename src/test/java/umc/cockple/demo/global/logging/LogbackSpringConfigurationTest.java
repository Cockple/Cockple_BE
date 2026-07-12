package umc.cockple.demo.global.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.spi.FilterAttachable;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;

import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LogbackSpringConfigurationTest {

    private static final String WEBSOCKET_LOGGER = "umc.cockple.demo.domain.chat.presentation.websocket";
    private static final String WEBSOCKET_SERVICE_LOGGER = "umc.cockple.demo.domain.chat.service.websocket";
    private static final String SPRING_WEBSOCKET_LOGGER = "org.springframework.web.socket";

    @TempDir
    Path logPath;

    @Test
    @DisplayName("prod 프로필에서는 WebSocket 로그가 파일 appender로만 라우팅된다")
    void prodProfileRoutesWebSocketLogsToFileOnly() throws Exception {
        LoggerContext context = configure("prod");

        try {
            assertThat(appenderNames(context.getLogger(WEBSOCKET_LOGGER)))
                    .containsExactlyInAnyOrder("ASYNC_WEBSOCKET_FILE", "ASYNC_ERROR_FILE");
            assertThat(context.getLogger(WEBSOCKET_LOGGER).isAdditive()).isFalse();
            assertThat(appenderNames(context.getLogger(WEBSOCKET_SERVICE_LOGGER)))
                    .containsExactlyInAnyOrder("ASYNC_WEBSOCKET_FILE", "ASYNC_ERROR_FILE");
            assertThat(appenderNames(context.getLogger(SPRING_WEBSOCKET_LOGGER)))
                    .containsExactlyInAnyOrder("ASYNC_WEBSOCKET_FILE", "ASYNC_ERROR_FILE");
            assertThat(appenderNames(context.getLogger(Logger.ROOT_LOGGER_NAME)))
                    .containsExactlyInAnyOrder("CONSOLE", "ASYNC_APPLICATION_FILE", "ASYNC_ERROR_FILE");
            assertThat(appenderPattern(context, "CONSOLE")).contains("%clr", "%highlight");
            assertThat(appenderPattern(context, "APPLICATION_FILE")).doesNotContain("%clr", "%highlight");
            assertThat(appenderPattern(context, "WEBSOCKET_FILE")).doesNotContain("%clr", "%highlight");
            assertThat(appenderPattern(context, "ERROR_FILE")).doesNotContain("%clr", "%highlight");
            assertHttpMdcPattern(appenderPattern(context, "CONSOLE"));
            assertHttpMdcPattern(appenderPattern(context, "APPLICATION_FILE"));
            assertHttpMdcPattern(appenderPattern(context, "WEBSOCKET_FILE"));
            assertHttpMdcPattern(appenderPattern(context, "ERROR_FILE"));
            assertAsyncAppender(context, "ASYNC_APPLICATION_FILE", "APPLICATION_FILE", 1024, true);
            assertAsyncAppender(context, "ASYNC_WEBSOCKET_FILE", "WEBSOCKET_FILE", 2048, true);
            assertAsyncAppender(context, "ASYNC_ERROR_FILE", "ERROR_FILE", 512, false);
            assertThresholdFilter(context, "ASYNC_ERROR_FILE");
            assertThresholdFilter(context, "ERROR_FILE");
        } finally {
            context.stop();
        }
    }

    @Test
    @DisplayName("local 프로필에서는 WebSocket 로그가 콘솔과 파일 appender에 함께 라우팅된다")
    void localProfileRoutesWebSocketLogsToConsoleAndFile() throws Exception {
        LoggerContext context = configure("local");

        try {
            assertThat(appenderNames(context.getLogger(WEBSOCKET_LOGGER)))
                    .containsExactlyInAnyOrder("CONSOLE", "ASYNC_WEBSOCKET_FILE", "ASYNC_ERROR_FILE");
            assertThat(context.getLogger(WEBSOCKET_LOGGER).isAdditive()).isFalse();
            assertThat(appenderNames(context.getLogger(WEBSOCKET_SERVICE_LOGGER)))
                    .containsExactlyInAnyOrder("CONSOLE", "ASYNC_WEBSOCKET_FILE", "ASYNC_ERROR_FILE");
            assertThat(appenderNames(context.getLogger(Logger.ROOT_LOGGER_NAME)))
                    .containsExactlyInAnyOrder("CONSOLE", "ASYNC_APPLICATION_FILE", "ASYNC_ERROR_FILE");
            assertThat(appenderPattern(context, "CONSOLE")).contains("%clr", "%highlight");
            assertThat(appenderPattern(context, "APPLICATION_FILE")).doesNotContain("%clr", "%highlight");
            assertThat(appenderPattern(context, "WEBSOCKET_FILE")).doesNotContain("%clr", "%highlight");
            assertThat(appenderPattern(context, "ERROR_FILE")).doesNotContain("%clr", "%highlight");
            assertHttpMdcPattern(appenderPattern(context, "CONSOLE"));
            assertHttpMdcPattern(appenderPattern(context, "APPLICATION_FILE"));
            assertHttpMdcPattern(appenderPattern(context, "WEBSOCKET_FILE"));
            assertHttpMdcPattern(appenderPattern(context, "ERROR_FILE"));
            assertAsyncAppender(context, "ASYNC_APPLICATION_FILE", "APPLICATION_FILE", 1024, true);
            assertAsyncAppender(context, "ASYNC_WEBSOCKET_FILE", "WEBSOCKET_FILE", 2048, true);
            assertAsyncAppender(context, "ASYNC_ERROR_FILE", "ERROR_FILE", 512, false);
            assertThresholdFilter(context, "ASYNC_ERROR_FILE");
            assertThresholdFilter(context, "ERROR_FILE");
        } finally {
            context.stop();
        }
    }

    @Test
    @DisplayName("test 프로필에서는 XML 테스트 리포트 보호를 위해 콘솔 색상을 사용하지 않는다")
    void testProfileUsesPlainConsolePattern() throws Exception {
        LoggerContext context = configure("test");

        try {
            assertThat(appenderNames(context.getLogger(Logger.ROOT_LOGGER_NAME)))
                    .containsExactlyInAnyOrder("CONSOLE", "ASYNC_APPLICATION_FILE", "ASYNC_ERROR_FILE");
            assertThat(appenderPattern(context, "CONSOLE")).doesNotContain("%clr", "%highlight");
            assertThat(appenderPattern(context, "APPLICATION_FILE")).doesNotContain("%clr", "%highlight");
            assertThat(appenderPattern(context, "ERROR_FILE")).doesNotContain("%clr", "%highlight");
            assertHttpMdcPattern(appenderPattern(context, "CONSOLE"));
            assertHttpMdcPattern(appenderPattern(context, "APPLICATION_FILE"));
            assertHttpMdcPattern(appenderPattern(context, "ERROR_FILE"));
        } finally {
            context.stop();
        }
    }

    @Test
    @DisplayName("명시 런타임 프로필이 아니면 기본 콘솔 패턴은 색상을 사용하지 않는다")
    void defaultProfileUsesPlainConsolePattern() throws Exception {
        LoggerContext context = configure();

        try {
            assertThat(appenderNames(context.getLogger(Logger.ROOT_LOGGER_NAME)))
                    .containsExactlyInAnyOrder("CONSOLE", "ASYNC_APPLICATION_FILE", "ASYNC_ERROR_FILE");
            assertThat(appenderPattern(context, "CONSOLE")).doesNotContain("%clr", "%highlight");
            assertHttpMdcPattern(appenderPattern(context, "CONSOLE"));
        } finally {
            context.stop();
        }
    }

    private LoggerContext configure() throws Exception {
        LoggerContext context = new LoggerContext();
        context.putProperty("LOG_PATH", logPath.toString());

        MockEnvironment environment = new MockEnvironment();
        LoggingInitializationContext initializationContext = new LoggingInitializationContext(environment);
        configureContext(context, initializationContext);
        return context;
    }

    private LoggerContext configure(String profile) throws Exception {
        LoggerContext context = new LoggerContext();
        context.putProperty("LOG_PATH", logPath.toString());

        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profile);
        LoggingInitializationContext initializationContext = new LoggingInitializationContext(environment);
        configureContext(context, initializationContext);
        return context;
    }

    private void configureContext(
            LoggerContext context,
            LoggingInitializationContext initializationContext
    ) throws Exception {
        Constructor<?> constructor = Class.forName("org.springframework.boot.logging.logback.SpringBootJoranConfigurator")
                .getDeclaredConstructor(LoggingInitializationContext.class);
        constructor.setAccessible(true);
        JoranConfigurator configurator = (JoranConfigurator) constructor.newInstance(initializationContext);
        configurator.setContext(context);
        configurator.doConfigure(new ClassPathResource("logback-spring.xml").getFile());

        StatusUtil statusUtil = new StatusUtil(context);
        assertThat(statusUtil.getHighestLevel(0)).isLessThan(Status.ERROR);
    }

    private List<String> appenderNames(Logger logger) {
        List<String> names = new ArrayList<>();
        Iterator<Appender<ch.qos.logback.classic.spi.ILoggingEvent>> iterator = logger.iteratorForAppenders();
        while (iterator.hasNext()) {
            Appender<?> appender = iterator.next();
            names.add(appender.getName());
        }
        return names;
    }

    private String appenderPattern(LoggerContext context, String appenderName) {
        Appender<?> appender = findAppender(context, appenderName);
        assertThat(appender).isInstanceOf(OutputStreamAppender.class);
        OutputStreamAppender<?> outputStreamAppender = (OutputStreamAppender<?>) appender;
        assertThat(outputStreamAppender.getEncoder()).isInstanceOf(PatternLayoutEncoder.class);
        PatternLayoutEncoder encoder = (PatternLayoutEncoder) outputStreamAppender.getEncoder();
        return encoder.getPattern();
    }

    private void assertHttpMdcPattern(String pattern) {
        assertThat(pattern).contains("%X{method:-}", "%X{uri:-}", "%X{clientIp:-}");
    }

    private void assertAsyncAppender(
            LoggerContext context,
            String asyncAppenderName,
            String nestedAppenderName,
            int queueSize,
            boolean neverBlock
    ) {
        Appender<?> appender = findAppender(context, asyncAppenderName);
        assertThat(appender).isInstanceOf(AsyncAppender.class);
        AsyncAppender asyncAppender = (AsyncAppender) appender;
        assertThat(asyncAppender.getQueueSize()).isEqualTo(queueSize);
        assertThat(asyncAppender.getDiscardingThreshold()).isZero();
        assertThat(asyncAppender.isNeverBlock()).isEqualTo(neverBlock);
        assertThat(asyncAppender.isIncludeCallerData()).isFalse();
        assertThat(asyncAppender.getAppender(nestedAppenderName)).isNotNull();
    }

    @SuppressWarnings("unchecked")
    private void assertThresholdFilter(LoggerContext context, String appenderName) {
        Appender<?> appender = findAppender(context, appenderName);
        assertThat(appender).isInstanceOf(FilterAttachable.class);
        FilterAttachable<ch.qos.logback.classic.spi.ILoggingEvent> filterAttachable =
                (FilterAttachable<ch.qos.logback.classic.spi.ILoggingEvent>) appender;
        assertThat(filterAttachable.getCopyOfAttachedFiltersList())
                .hasSize(1)
                .allMatch(ThresholdFilter.class::isInstance);
    }

    private Appender<?> findAppender(LoggerContext context, String appenderName) {
        List<Logger> loggers = List.of(
                context.getLogger(Logger.ROOT_LOGGER_NAME),
                context.getLogger(WEBSOCKET_LOGGER),
                context.getLogger(WEBSOCKET_SERVICE_LOGGER),
                context.getLogger(SPRING_WEBSOCKET_LOGGER)
        );

        for (Logger logger : loggers) {
            Appender<?> directAppender = logger.getAppender(appenderName);
            if (directAppender != null) {
                return directAppender;
            }
        }

        for (Logger logger : loggers) {
            Iterator<Appender<ch.qos.logback.classic.spi.ILoggingEvent>> iterator = logger.iteratorForAppenders();
            while (iterator.hasNext()) {
                Appender<?> appender = iterator.next();
                if (appender instanceof AsyncAppender asyncAppender) {
                    Appender<?> nestedAppender = asyncAppender.getAppender(appenderName);
                    if (nestedAppender != null) {
                        return nestedAppender;
                    }
                }
            }
        }

        return null;
    }
}
