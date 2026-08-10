package umc.cockple.demo.global.realtime.publish;

public interface RealtimeMessagePublisher {

    RealtimePublishResult publish(
            Long memberId,
            String domain,
            String type,
            Object data
    );
}
