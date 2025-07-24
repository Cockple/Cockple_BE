package umc.cockple.demo.domain.party.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import umc.cockple.demo.global.response.code.BaseErrorCode;
import umc.cockple.demo.global.response.dto.ErrorReasonDTO;

@Getter
@RequiredArgsConstructor
public enum PartyErrorCode implements BaseErrorCode {

    /**
     * 1xx: 클라이언트가 수정해야 할 입력값 문제
     * 2xx: 서버에서 리소스를 찾을 수 없는 문제
     * 3xx: 권한/인증 문제
     * 4xx: 비즈니스 로직 위반
     */
    INVALID_PARTY_TYPE(HttpStatus.BAD_REQUEST, "PARTY101", "유효하지 않은 모임 유형입니다. (WOMEN_DOUBLES 또는 MIX_DOUBLES을 입력해주세요.)"),
    INVALID_ACTIVITY_DAY(HttpStatus.BAD_REQUEST, "PARTY102", "유효하지 않은 활동 요일입니다. (SUNDAY~SATURDAY를 리스트로 입력해주세요.)"),
    INVALID_ACTIVITY_TIME(HttpStatus.BAD_REQUEST, "PARTY103", "유효하지 않은 활동 시간입니다. (MORNING 또는 AFTERNOON 또는 ALWAYS를 입력해주세요.)"),
    INVALID_LEVEL_FORMAT(HttpStatus.BAD_REQUEST, "PARTY104", "유효하지 않은 급수 형식입니다."),
    MALE_LEVEL_REQUIRED(HttpStatus.BAD_REQUEST, "PARTY105", "혼복 모임은 남녀 급수 설정이 필수입니다."),
    INVALID_REQUEST_STATUS(HttpStatus.BAD_REQUEST, "PARTY106", "유효하지 않은 가입 신청 상태입니다. (PENDING 또는 APPROVED를 입력해주세요.)"),
    INVALID_ORDER_TYPE(HttpStatus.BAD_REQUEST, "PARTY107", "유효하지 않은 정렬 기준입니다. (최신순, 오래된 순, 운동 많은 순 중 하나여야 합니다.)"),

    PARTY_NOT_FOUND(HttpStatus.NOT_FOUND, "PARTY201", "존재하지 않는 모임입니다."),
    JoinRequest_NOT_FOUND(HttpStatus.NOT_FOUND, "PARTY202", "존재하지 않는 가입신청입니다."),
    JOIN_REQUEST_PARTY_NOT_FOUND(HttpStatus.NOT_FOUND, "PARTY203", "해당 모임에서 존재하지 않는 가입신청입니다."),

    INSUFFICIENT_PERMISSION(HttpStatus.FORBIDDEN, "PARTY301", "해당 작업을 수행할 권한이 없습니다."),

    ALREADY_MEMBER(HttpStatus.CONFLICT, "PARTY401", "이미 가입된 모임입니다."),
    JOIN_REQUEST_ALREADY_EXISTS(HttpStatus.CONFLICT, "PARTY402", "처리 대기중인 가입 신청이 존재합니다."),
    JOIN_REQUEST_ALREADY_ACTIONS(HttpStatus.CONFLICT, "PARTY403", "이미 처리된 가입 신청입니다."),
    LEVEL_NOT_MATCH(HttpStatus.BAD_REQUEST, "PARTY404", "모임의 급수 조건에 맞지 않습니다."),
    GENDER_NOT_MATCH(HttpStatus.BAD_REQUEST, "PARTY405", "모임 유형에 맞지 않는 성별입니다."),
    AGE_NOT_MATCH(HttpStatus.BAD_REQUEST, "PARTY406", "모임의 나이 조건에 맞지 않습니다."),
    PARTY_IS_DELETED(HttpStatus.BAD_REQUEST, "PARTY407", "이미 삭제된 모임입니다.");


    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReasonDTO getReason() {
        return ErrorReasonDTO.of(code, message, httpStatus);
    }
}
