package umc.cockple.demo.domain.party.exception;

import umc.cockple.demo.global.exception.GeneralException;
import umc.cockple.demo.global.response.code.BaseErrorCode;

public class PartyException extends GeneralException {

    public PartyException(BaseErrorCode code) {
        super(code);
    }
}
