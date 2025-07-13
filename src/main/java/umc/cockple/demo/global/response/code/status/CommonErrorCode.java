package umc.cockple.demo.global.response.code.status;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import umc.cockple.demo.global.response.code.BaseErrorCode;
import umc.cockple.demo.global.response.dto.ErrorReasonDTO;

@Getter
@AllArgsConstructor
public enum CommonErrorCode implements BaseErrorCode {

    BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON400", "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON401", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON403", "접근 권한이 없는 요청입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON404", "요청 리소스를 찾을 수 없습니다"),

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON500", "서버 에러, 관리자에게 문의 바랍니다."),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "COMMON503", "서버가 일시적으로 사용중지 되었습니다."),

    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "COMMON400_VALIDATION", "입력값 검증에 실패했습니다"),
    INVALID_PARAMETER_TYPE(HttpStatus.BAD_REQUEST, "COMMON400_TYPE", "파라미터 타입이 올바르지 않습니다"),
    INVALID_REQUEST_FORMAT(HttpStatus.BAD_REQUEST, "COMMON400_FORMAT", "요청 형식이 올바르지 않습니다"),
    MISSING_REQUIRED_PARAMETER(HttpStatus.BAD_REQUEST, "COMMON400_PARAM", "필수 파라미터가 누락되었습니다"),

    INVALID_GENDER_FORMAT(HttpStatus.BAD_REQUEST, "COMMON400_GENDER", "성별은 '남성' 또는 '여성'이어야 합니다."),
    INVALID_LEVEL_FORMAT(HttpStatus.BAD_REQUEST, "COMMON400_LEVEL", "올바른 급수를 입력해주세요. (자강, 준자강, A조, B조, C조, D조, 초심, 왕초심, 급수없음)");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReasonDTO getReason() {
        return ErrorReasonDTO.of(code, message, httpStatus);
    }
}
