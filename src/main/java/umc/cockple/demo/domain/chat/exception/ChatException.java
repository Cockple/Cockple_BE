package umc.cockple.demo.domain.chat.exception;

import umc.cockple.demo.global.exception.GeneralException;
import umc.cockple.demo.global.response.code.BaseErrorCode;

public class ChatException extends GeneralException {

    public ChatException(BaseErrorCode code) {
        super(code);
    }
}
