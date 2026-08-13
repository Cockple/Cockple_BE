package umc.cockple.demo.global.realtime.routing;

public enum RealtimeRoutingErrorCode {
    INVALID_MESSAGE("INVALID_MESSAGE", "실시간 요청 형식이 올바르지 않습니다."),
    MESSAGE_TOO_LARGE("MESSAGE_TOO_LARGE", "실시간 요청 크기가 허용 범위를 초과했습니다."),
    UNAUTHORIZED("UNAUTHORIZED", "인증되지 않은 사용자입니다."),
    UNSUPPORTED_VERSION("UNSUPPORTED_VERSION", "지원하지 않는 실시간 프로토콜 버전입니다."),
    UNKNOWN_ROUTE("UNKNOWN_ROUTE", "처리할 수 없는 실시간 요청입니다."),
    INTERNAL_ERROR("INTERNAL_ERROR", "실시간 요청 처리 중 오류가 발생했습니다.");

    private final String code;
    private final String message;

    RealtimeRoutingErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
