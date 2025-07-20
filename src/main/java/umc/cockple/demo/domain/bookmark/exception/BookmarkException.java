package umc.cockple.demo.domain.bookmark.exception;

import umc.cockple.demo.global.exception.GeneralException;
import umc.cockple.demo.global.response.code.BaseErrorCode;

public class BookmarkException extends GeneralException {

    public BookmarkException(BaseErrorCode code) { super(code); }
}
