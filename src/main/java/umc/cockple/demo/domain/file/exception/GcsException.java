package umc.cockple.demo.domain.file.exception;

import umc.cockple.demo.global.exception.GeneralException;
import umc.cockple.demo.global.response.code.BaseErrorCode;

public class GcsException extends GeneralException {

    public GcsException(BaseErrorCode code) { super(code); }
}
