package umc.cockple.demo.domain.image.exception;

import umc.cockple.demo.global.exception.GeneralException;
import umc.cockple.demo.global.response.code.BaseErrorCode;

public class S3Exception extends GeneralException {

    public S3Exception(BaseErrorCode code) { super(code); }
}
