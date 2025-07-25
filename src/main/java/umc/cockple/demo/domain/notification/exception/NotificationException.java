package umc.cockple.demo.domain.notification.exception;

import umc.cockple.demo.global.exception.GeneralException;
import umc.cockple.demo.global.response.code.BaseErrorCode;

public class NotificationException extends GeneralException {

    public NotificationException(BaseErrorCode code) {
        super(code);
    }
}
