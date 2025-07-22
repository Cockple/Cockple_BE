package umc.cockple.demo.global.enums;

import lombok.Getter;
import umc.cockple.demo.domain.bookmark.exception.BookmarkErrorCode;
import umc.cockple.demo.domain.bookmark.exception.BookmarkException;

import java.util.Arrays;

public enum ExerciseOrderType {

    LATEST, // 최신순
    EARLIEST // 오래된순

}
