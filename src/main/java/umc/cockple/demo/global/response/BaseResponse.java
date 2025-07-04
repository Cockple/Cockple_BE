package umc.cockple.demo.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import umc.cockple.demo.global.response.code.BaseErrorCode;
import umc.cockple.demo.global.response.dto.ErrorReasonDTO;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)  // null 값 제외
public class BaseResponse<T> {

    private final boolean isSuccess;
    private final String code;
    private final String message;
    private final T data;
    private final ErrorReasonDTO errorReason;

    public static <T> BaseResponse<T> error(BaseErrorCode errorCode) {
        ErrorReasonDTO errorReason = errorCode.getReason();
        return BaseResponse.<T>builder()
                .isSuccess(false)
                .code(errorReason.getCode())
                .message(errorReason.getMessage())
                .errorReason(errorReason)
                .build();
    }

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
