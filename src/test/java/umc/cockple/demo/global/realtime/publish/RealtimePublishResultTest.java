package umc.cockple.demo.global.realtime.publish;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RealtimePublishResult")
class RealtimePublishResultTest {

    @Test
    @DisplayName("성공 세션 수가 대상 세션 수보다 많으면 생성할 수 없다")
    void rejectsInvalidCounts() {
        assertThatThrownBy(() -> new RealtimePublishResult(1, 2))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
