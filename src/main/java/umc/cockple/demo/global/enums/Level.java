package umc.cockple.demo.global.enums;

import umc.cockple.demo.global.exception.GeneralException;
import umc.cockple.demo.global.response.code.status.CommonErrorCode;

import java.util.Arrays;

public enum Level {

    EXPERT("자강"),
    SEMI_EXPERT("준자강"),
    A("A조"),
    B("B조"),
    C("C조"),
    D("D조"),
    BEGINNER("초심"),
    NOVICE("왕초심"),
    NONE("급수없음");

    private final String koreanName;

    Level(String koreanName) {
        this.koreanName = koreanName;
    }

    public static Level fromKorean(String korean) {
        return Arrays.stream(values())
                .filter(level -> level.koreanName.equals(korean.trim()))
                .findFirst()
                .orElseThrow(() -> new GeneralException(CommonErrorCode.INVALID_LEVEL_FORMAT));
    }

}
