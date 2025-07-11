package umc.cockple.demo.domain.contest.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import umc.cockple.demo.global.response.code.BaseErrorCode;
import umc.cockple.demo.global.response.dto.ErrorReasonDTO;

@Getter
@RequiredArgsConstructor
public enum ContestErrorCode implements BaseErrorCode {

    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "CONTEST201", "존재하지 않는 회원입니다."),

    //Save
    IMAGE_UPLOAD_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "CONTEST901", "이미지 업로드에 실패했습니다."),
    VIDEO_URL_SAVE_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "CONTEST902", "영상 URL 저장에 실패했습니다."),
    CONTEST_SAVE_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "CONTEST903", "대회 기록 저장 중 문제가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReasonDTO getReason() {
        return ErrorReasonDTO.of(code, message, httpStatus);
    }
}
