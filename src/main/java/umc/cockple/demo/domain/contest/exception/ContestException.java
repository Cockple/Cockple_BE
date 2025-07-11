package umc.cockple.demo.domain.contest.exception;

import umc.cockple.demo.global.exception.GeneralException;
import umc.cockple.demo.global.response.code.BaseErrorCode;

public class ContestException extends GeneralException {

    public ContestException(BaseErrorCode code) {
        super(code);
    }
}
