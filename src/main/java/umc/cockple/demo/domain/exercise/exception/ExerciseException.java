package umc.cockple.demo.domain.exercise.exception;

import umc.cockple.demo.global.exception.GeneralException;
import umc.cockple.demo.global.response.code.BaseErrorCode;

public class ExerciseException extends GeneralException {

    public ExerciseException(BaseErrorCode code) {
        super(code);
    }
}
