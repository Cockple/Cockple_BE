package umc.cockple.demo.global.enums;

import umc.cockple.demo.global.exception.GeneralException;
import umc.cockple.demo.global.response.code.status.CommonErrorCode;

import java.util.Arrays;

public enum Gender {
    MALE("남성"),
    FEMALE("여성");

    private final String koreanName;

    Gender(String koreanName) {
        this.koreanName = koreanName;
    }

    public static Gender fromKorean(String korean) {
        return Arrays.stream(values())
                .filter(gender -> gender.koreanName.equals(korean.trim()))
                .findFirst()
                .orElseThrow(() -> new GeneralException(CommonErrorCode.INVALID_GENDER_FORMAT));
    }
}
