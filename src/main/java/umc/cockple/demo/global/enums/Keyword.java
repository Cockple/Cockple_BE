package umc.cockple.demo.global.enums;

import lombok.Getter;

public enum Keyword {

    BRAND("브랜드 스폰"),
    FREE("가입비 무료"),
    FRIENDSHIP("친목"),
    MANAGER_MATCH("운영진이 게임을 짜드려요"),
    NONE("선택안함");

    @Getter
    private final String koreanName;

    Keyword(String koreanName) {
        this.koreanName = koreanName;
    }
}
