package umc.cockple.demo.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import umc.cockple.demo.global.response.BaseResponse;
import umc.cockple.demo.global.response.code.status.CommonErrorCode;
import umc.cockple.demo.global.response.dto.ErrorReasonDTO;

import java.util.stream.Collectors;

import static java.util.Arrays.stream;

@Slf4j
@RestControllerAdvice(annotations = {RestController.class})
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * 비즈니스 로직 예외
     */
    @ExceptionHandler(GeneralException.class)
    public ResponseEntity<BaseResponse<Void>> handleGeneralException(
            GeneralException ex, WebRequest request) {

        ErrorReasonDTO errorReason = ex.getErrorReason();

        log.warn("Business Exception: code={}, message={}, uri={}",
                errorReason.getCode(), errorReason.getMessage(), getRequestURI(request));

        BaseResponse<Void> response = BaseResponse.error(ex.getCode());

        return ResponseEntity
                .status(errorReason.getHttpStatus())
                .body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Void>> handleGlobalException(
            Exception ex, WebRequest request){

        String requestURI = getRequestURI(request);

        log.error("Unexpected exception: uri={}, type={}, message={}, stackTrace={}",
                requestURI, ex.getClass().getSimpleName(), ex.getMessage(), getStackTrace(ex));

        BaseResponse<Void> response = BaseResponse.error(CommonErrorCode.INTERNAL_SERVER_ERROR);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    private static String getRequestURI(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }

    private String getStackTrace(Exception ex) {
        return stream(ex.getStackTrace())
                .limit(5)  // 상위 5개만
                .map(StackTraceElement::toString)
                .collect(Collectors.joining(" | "));
    }

}
