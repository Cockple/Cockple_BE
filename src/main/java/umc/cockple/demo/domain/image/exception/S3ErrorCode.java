package umc.cockple.demo.domain.image.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import umc.cockple.demo.global.response.code.BaseErrorCode;
import umc.cockple.demo.global.response.dto.ErrorReasonDTO;

@Getter
@RequiredArgsConstructor
public enum S3ErrorCode implements BaseErrorCode {

    /**
     * 1xx: 클라이언트가 수정해야 할 입력값 문제
     * 2xx: 서버에서 리소스를 찾을 수 없는 문제
     * 3xx: 권한/인증 문제
     * 4xx: 비즈니스 로직 위반
     */
    IMAGE_UPLOAD_AMAZON_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, "IMG501", "이미지 업로드 중, AWS 예외가 발생하였습니다. 서버 관리자에게 문의해주세요"),
    IMAGE_UPLOAD_IO_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, "IMG502", "이미지 업로드 중, IO 예외가 발생하였습니다. 서버 관리자에게 문의해주세요"),
    IMAGE_STILL_EXIST(HttpStatus.INTERNAL_SERVER_ERROR,"IMG503" ,"이미지가 삭제되지 않고 S3에 남아있습니다. 서버 관리자에게 문의해주세요" ),
    IMAGE_DELETE_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR,"IMG504" ,"이미지 삭제에 실패하였습니다. 서버관리자에게 문의해주세요" ),
    IMAGE_BUCKET_DIRECTORY_NULL(HttpStatus.BAD_REQUEST, "IMG505", "버킷 디렉토리 값이 유효하지 않습니다."),
    FILE_UPLOAD_AMAZON_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, "IMG506", "파일 업로드 중, AWS 예외가 발생하였습니다. 서버 관리자에게 문의해주세요"),
    FILE_UPLOAD_IO_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, "IMG507", "파일 업로드 중, IO 예외가 발생하였습니다. 서버 관리자에게 문의해주세요"),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReasonDTO getReason() {
        return ErrorReasonDTO.of(code, message, httpStatus);
    }
}