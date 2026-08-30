package umc.cockple.demo.domain.game.exception;

import umc.cockple.demo.global.exception.GeneralException;
import umc.cockple.demo.global.response.code.BaseErrorCode;

public class GameException extends GeneralException {

    public GameException(BaseErrorCode code) {
        super(code);
    }
}
