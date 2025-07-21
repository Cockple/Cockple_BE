package umc.cockple.demo.domain.bookmark.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import umc.cockple.demo.global.response.code.BaseErrorCode;
import umc.cockple.demo.global.response.dto.ErrorReasonDTO;

@Getter
@RequiredArgsConstructor
public enum BookmarkErrorCode implements BaseErrorCode {

    /**
     * 1xx: 클라이언트가 수정해야 할 입력값 문제
     * 2xx: 서버에서 리소스를 찾을 수 없는 문제
     * 3xx: 권한/인증 문제
     * 4xx: 비즈니스 로직 위반
     */

    ALREADY_BOOKMARK(HttpStatus.BAD_REQUEST, "BOOKMARK401", "이미 북마크가 되어있습니다"),
    ALREADY_RELEASE_BOOKMARK(HttpStatus.BAD_REQUEST, "BOOKMARK402", "이미 찜이 되어있지 않습니다."),


    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReasonDTO getReason() {
        return ErrorReasonDTO.of(code, message, httpStatus);
    }
}