package umc.cockple.demo.global.response.code.status;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import umc.cockple.demo.global.response.code.BaseCode;
import umc.cockple.demo.global.response.dto.ReasonDTO;

@Getter
@RequiredArgsConstructor
public enum CommonSuccessCode implements BaseCode {

    OK(HttpStatus.OK, "COMMON200", "요청에 성공했습니다."),
    CREATED(HttpStatus.CREATED, "COMMON201", "요청이 성공적으로 처리되어 리소스가 생성되었습니다."),
    ACCEPTED(HttpStatus.ACCEPTED, "COMMON202", "요청이 접수되었습니다."),
    NO_CONTENT(HttpStatus.NO_CONTENT, "COMMON204", "요청이 성공적으로 처리되었으나 반환할 데이터가 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ReasonDTO getReason(){
        return ReasonDTO.of(code, message, httpStatus);
    }
}
