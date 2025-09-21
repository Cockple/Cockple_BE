package umc.cockple.demo.domain.member.exception;

import umc.cockple.demo.global.exception.GeneralException;
import umc.cockple.demo.global.response.code.BaseErrorCode;

public class MemberException extends GeneralException {

    public MemberException(BaseErrorCode code) { super(code); }
}
