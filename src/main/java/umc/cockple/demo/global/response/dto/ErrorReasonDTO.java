package umc.cockple.demo.global.response.dto;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@Builder
public class ErrorReasonDTO {

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    public static ErrorReasonDTO of(String code, String message, HttpStatus httpStatus) {
        return ErrorReasonDTO.builder()
                .code(code)
                .message(message)
                .httpStatus(httpStatus)
                .build();
    }



}
