package umc.cockple.demo.global.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.OutputStreamAppender;
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
            assertThat(appenderNames(context.getLogger(WEBSOCKET_LOGGER))).containsExactly("WEBSOCKET_FILE");
            assertThat(context.getLogger(WEBSOCKET_LOGGER).isAdditive()).isFalse();
            assertThat(appenderNames(context.getLogger(WEBSOCKET_SERVICE_LOGGER))).containsExactly("WEBSOCKET_FILE");
            assertThat(appenderNames(context.getLogger(SPRING_WEBSOCKET_LOGGER))).containsExactly("WEBSOCKET_FILE");
            assertThat(appenderNames(context.getLogger(Logger.ROOT_LOGGER_NAME)))
                    .containsExactlyInAnyOrder("CONSOLE", "APPLICATION_FILE");
            assertThat(appenderPattern(context, "CONSOLE")).contains("%clr", "%highlight");
            assertThat(appenderPattern(context, "APPLICATION_FILE")).doesNotContain("%clr", "%highlight");
            assertThat(appenderPattern(context, "WEBSOCKET_FILE")).doesNotContain("%clr", "%highlight");
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
                    .containsExactlyInAnyOrder("CONSOLE", "WEBSOCKET_FILE");
            assertThat(context.getLogger(WEBSOCKET_LOGGER).isAdditive()).isFalse();
            assertThat(appenderNames(context.getLogger(WEBSOCKET_SERVICE_LOGGER)))
                    .containsExactlyInAnyOrder("CONSOLE", "WEBSOCKET_FILE");
            assertThat(appenderNames(context.getLogger(Logger.ROOT_LOGGER_NAME)))
                    .containsExactlyInAnyOrder("CONSOLE", "APPLICATION_FILE");
            assertThat(appenderPattern(context, "CONSOLE")).contains("%clr", "%highlight");
            assertThat(appenderPattern(context, "APPLICATION_FILE")).doesNotContain("%clr", "%highlight");
            assertThat(appenderPattern(context, "WEBSOCKET_FILE")).doesNotContain("%clr", "%highlight");
        } finally {
            context.stop();
        }
    }

    private LoggerContext configure(String profile) throws Exception {
        LoggerContext context = new LoggerContext();
        context.putProperty("LOG_PATH", logPath.toString());

        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profile);
        LoggingInitializationContext initializationContext = new LoggingInitializationContext(environment);

        Constructor<?> constructor = Class.forName("org.springframework.boot.logging.logback.SpringBootJoranConfigurator")
                .getDeclaredConstructor(LoggingInitializationContext.class);
        constructor.setAccessible(true);
        JoranConfigurator configurator = (JoranConfigurator) constructor.newInstance(initializationContext);
        configurator.setContext(context);
        configurator.doConfigure(new ClassPathResource("logback-spring.xml").getFile());

        StatusUtil statusUtil = new StatusUtil(context);
        assertThat(statusUtil.getHighestLevel(0)).isLessThan(Status.ERROR);
        return context;
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
        Appender<?> appender = context.getLogger(Logger.ROOT_LOGGER_NAME).getAppender(appenderName);
        if (appender == null) {
            appender = context.getLogger(WEBSOCKET_LOGGER).getAppender(appenderName);
        }
        assertThat(appender).isInstanceOf(OutputStreamAppender.class);
        OutputStreamAppender<?> outputStreamAppender = (OutputStreamAppender<?>) appender;
        assertThat(outputStreamAppender.getEncoder()).isInstanceOf(PatternLayoutEncoder.class);
        PatternLayoutEncoder encoder = (PatternLayoutEncoder) outputStreamAppender.getEncoder();
        return encoder.getPattern();
    }
}
