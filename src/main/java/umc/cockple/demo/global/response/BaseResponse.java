package umc.cockple.demo.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import umc.cockple.demo.global.response.code.BaseCode;
import umc.cockple.demo.global.response.code.BaseErrorCode;
import umc.cockple.demo.global.response.code.status.CommonSuccessCode;
import umc.cockple.demo.global.response.dto.ErrorReasonDTO;
import umc.cockple.demo.global.response.dto.ReasonDTO;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)  // null 값 제외
public class BaseResponse<T> {

    private final boolean isSuccess;
    private final String code;
    private final String message;
    private final T data;
    private final ErrorReasonDTO errorReason;

    //성공 응답 생성: 결과 데이터 포함
    public static <T> BaseResponse<T> success(BaseCode successCode, T data) {
        ReasonDTO reason = successCode.getReason();
        return BaseResponse.<T>builder()
                .isSuccess(true)
                .code(reason.getCode())
                .message(reason.getMessage())
                .data(data)
                .build();
    }

    //성공 응답 생성: 결과 데이터 없음
    public static <T> BaseResponse<T> success(BaseCode successCode) {
        ReasonDTO reason = successCode.getReason();
        return BaseResponse.<T>builder()
                .isSuccess(true)
                .code(reason.getCode())
                .message(reason.getMessage())
                .build();
    }

    //성공 응답 생성: 결과 데이터 포함, 커스텀 메시지 포함
    public static <T> BaseResponse<T> success(BaseCode successCode, String customMessage, T data) {
        ReasonDTO reason = successCode.getReason();
        return BaseResponse.<T>builder()
                .isSuccess(true)
                .code(reason.getCode())
                .message(customMessage)
                .data(data)
                .build();
    }

    //성공 응답 생성: 결과 데이터 없음, 커스텀 메시지 포함
    public static <T> BaseResponse<T> success(BaseCode successCode, String customMessage) {
        ReasonDTO reason = successCode.getReason();
        return BaseResponse.<T>builder()
                .isSuccess(true)
                .code(reason.getCode())
                .message(customMessage)
                .build();
    }

    //실패 응답 생성
    public static <T> BaseResponse<T> error(BaseErrorCode errorCode) {
        ErrorReasonDTO errorReason = errorCode.getReason();
        return BaseResponse.<T>builder()
                .isSuccess(false)
                .code(errorReason.getCode())
                .message(errorReason.getMessage())
                .errorReason(errorReason)
                .build();
    }

    //실패 응답 생성: 커스텀 메시지 포함
    public static <T> BaseResponse<T> error(BaseErrorCode errorCode, String customMessage) {
        ErrorReasonDTO errorReason = errorCode.getReason();
        return BaseResponse.<T>builder()
                .isSuccess(false)
                .code(errorReason.getCode())
                .message(customMessage)
                .errorReason(errorReason)
                .build();
    }

}
