package umc.cockple.demo.global.realtime.config;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "cockple.realtime.websocket")
@Validated
@Getter
@Setter
public class RealtimeWebSocketProperties {

    @Min(1)
    private int maxPayloadLength = 65_536;
}
