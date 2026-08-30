package umc.cockple.demo.global.realtime.publish;

public interface RealtimeMessagePublisher {

    RealtimePublishResult publish(
            Long memberId,
            String domain,
            String type,
            Object data
    );

    /**
     * 회원의 특정 세션 하나에만 발행한다.
     * 같은 회원이 여러 세션(탭/기기)을 열어둔 경우, 구독한 세션에만 전달해야 할 때 사용한다.
     */
    RealtimePublishResult publishToSession(
            Long memberId,
            String sessionId,
            String domain,
            String type,
            Object data
    );
}
