package umc.cockple.demo.domain.member.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import umc.cockple.demo.global.response.code.BaseErrorCode;
import umc.cockple.demo.global.response.dto.ErrorReasonDTO;

@Getter
@RequiredArgsConstructor
public enum MemberErrorCode implements BaseErrorCode {

    /**
     * 1xx: 클라이언트가 수정해야 할 입력값 문제
     * 2xx: 서버에서 리소스를 찾을 수 없는 문제
     * 3xx: 권한/인증 문제
     * 4xx: 비즈니스 로직 위반
     */

    // 회원 관련
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER201", "해당 사용자를 찾을 수 없습니다."),

    ALREADY_WITHDRAW(HttpStatus.UNAUTHORIZED, "MEMBER301", "이미 탈퇴한 회원입니다."),

    MANAGER_CANNOT_LEAVE(HttpStatus.BAD_REQUEST, "MEMBER401", "모임장은 탈퇴할 수 없습니다. 모임 삭제를 먼저 해주세요."),

    // 회원 주소 관련
    ADDRESS_NOT_FOUND(HttpStatus.NOT_FOUND, "MEM_ADDR201", "해당 주소를 찾을 수 없습니다."),

    DUPLICATE_ADDRESS(HttpStatus.BAD_REQUEST, "MEM_ADDR401", "이미 같은 주소가 존재합니다."),
    OVER_NUMBER_OF_ADDR(HttpStatus.BAD_REQUEST, "MEM_ADDR402", "주소 개수가 5개를 초과합니다."),
    CANNOT_REMOVE_MAIN_ADDR(HttpStatus.BAD_REQUEST, "MEM_ADDR403", "대표주소는 삭제할 수 없습니다."),
    MEMBER_ADDRESS_MINIMUM_REQUIRED(HttpStatus.BAD_REQUEST, "MEM_ADDR404", "주소가 적어도 1개 이상 필요합니다."),
    MAIN_ADDRESS_NULL(HttpStatus.BAD_REQUEST, "MEM_ADDR405", "대표 주소가 존재하지 않습니다."),

    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReasonDTO getReason() {
        return ErrorReasonDTO.of(code, message, httpStatus);
    }
}