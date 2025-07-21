package umc.cockple.demo.global.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import umc.cockple.demo.global.response.BaseResponse;
import umc.cockple.demo.global.response.code.status.CommonErrorCode;
import umc.cockple.demo.global.response.dto.ErrorReasonDTO;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

@Slf4j
@RestControllerAdvice(annotations = {RestController.class})
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * 비즈니스 로직 예외
     */
    @ExceptionHandler(GeneralException.class)
    public ResponseEntity<Object> handleGeneralException(
            GeneralException ex, WebRequest request) {

        ErrorReasonDTO errorReason = ex.getErrorReason();

        log.warn("Business Exception: code={}, message={}, uri={}",
                errorReason.getCode(), errorReason.getMessage(), getRequestURI(request));

        BaseResponse<Void> response = BaseResponse.error(ex.getCode());

        return ResponseEntity
                .status(errorReason.getHttpStatus())
                .body(response);
    }

    /**
     * @Valid 어노테이션으로 binding error 발생 시 (@RequestBody)
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        String requestURI = getRequestURI(request);
        List<String> fieldErrors = extractFieldErrors(ex);

        log.warn("Validation failed: uri={}, errors={}", requestURI, fieldErrors);

        String errorMessage = buildAllErrorMessages(ex);
        BaseResponse<Void> response = BaseResponse.error(CommonErrorCode.VALIDATION_FAILED, errorMessage);

        return ResponseEntity
                .status(CommonErrorCode.VALIDATION_FAILED.getHttpStatus())
                .body(response);
    }

    private static List<String> extractFieldErrors(MethodArgumentNotValidException ex) {
        return ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
    }

    private static String buildAllErrorMessages(MethodArgumentNotValidException ex) {
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();

        if (fieldErrors.isEmpty()) {
            return "입력값 검증에 실패했습니다.";
        }

        List<String> errorMessages = fieldErrors.stream()
                .map(FieldError::getDefaultMessage)
                .distinct()
                .toList();

        if (errorMessages.size() == 1) {
            return errorMessages.get(0);
        } else {
            return String.join(", ", errorMessages);
        }
    }

    /**
     * @Validated 어노테이션으로 binding error 발생 시 (@PathVariable, @RequestParam)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(
            ConstraintViolationException ex, WebRequest request) {

        String requestURI = getRequestURI(request);
        List<String> violations = extractConstraintViolations(ex);

        log.warn("Constraint violation: uri={}, message={}", requestURI, violations);

        String errorMessage = buildErrorMessage(ex);
        BaseResponse<Void> response = BaseResponse.error(CommonErrorCode.VALIDATION_FAILED, errorMessage);

        return ResponseEntity
                .status(CommonErrorCode.VALIDATION_FAILED.getHttpStatus())
                .body(response);
    }

    private static List<String> extractConstraintViolations(ConstraintViolationException ex) {
        return ex.getConstraintViolations().stream()
                .map(violation -> {
                    String propertyPath = violation.getPropertyPath().toString();
                    String message = violation.getMessage();
                    return propertyPath + ": " + message;
                })
                .toList();
    }

    private static String buildErrorMessage(ConstraintViolationException ex) {
        return ex.getConstraintViolations().stream()
                .findFirst()
                .map(ConstraintViolation::getMessage)
                .orElse("요청 파라미터가 올바르지 않습니다.");
    }

    /**
     * RequestParam 타입 변환 실패
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex, WebRequest request) {

        String requestURI = getRequestURI(request);
        String expectedType = getExpectedType(ex);

        log.warn("Type mismatch: uri={}, param={}, value={}, expectedType={}",
                requestURI, ex.getName(), ex.getValue(), expectedType);

        String errorMessage = buildErrorMessage(ex, expectedType);
        BaseResponse<Void> response = BaseResponse.error(CommonErrorCode.INVALID_PARAMETER_TYPE, errorMessage);

        return ResponseEntity
                .status(CommonErrorCode.INVALID_PARAMETER_TYPE.getHttpStatus())
                .body(response);
    }

    private static String buildErrorMessage(MethodArgumentTypeMismatchException ex, String expectedType) {
        return String.format("파라미터 '%s'는 %s 타입이어야 합니다.",
                ex.getName(), expectedType);
    }

    private static String getExpectedType(MethodArgumentTypeMismatchException ex) {
        return Optional.ofNullable(ex.getRequiredType())
                .map(Class::getSimpleName)
                .orElse("unknown");
    }

    /**
     * 필수 RequestParam 누락
     */
    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        String requestURI = getRequestURI(request);

        log.warn("Missing parameter: uri={}, param={}", requestURI, ex.getParameterName());

        String errorMessage = String.format("필수 파라미터 '%s'가 누락되었습니다.", ex.getParameterName());
        BaseResponse<Void> response = BaseResponse.error(CommonErrorCode.MISSING_REQUIRED_PARAMETER, errorMessage);

        return ResponseEntity
                .status(CommonErrorCode.MISSING_REQUIRED_PARAMETER.getHttpStatus())
                .body(response);
    }

    /**
     * JSON 파싱 오류 (@RequestBody)
     */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        String requestURI = getRequestURI(request);

        log.warn("JSON parsing error: uri={}, message={}", requestURI, ex.getMessage());

        BaseResponse<Void> response = BaseResponse.error(
                CommonErrorCode.INVALID_REQUEST_FORMAT,
                "요청 데이터 형식이 올바르지 않습니다."
        );

        return ResponseEntity
                .status(CommonErrorCode.INVALID_REQUEST_FORMAT.getHttpStatus())
                .body(response);
    }

    /**
     * 최종 예외
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGlobalException(
            Exception ex, WebRequest request) {

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
