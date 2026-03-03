package umc.cockple.demo.support.fixture;

import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberAddr;

public class MemberAddrFixture {

    /**
     * 임의 테스트 주소
     * 대표주소
     * - 서울특별시 강남구 역삼동 테헤란로 123 / ㅁㅁ빌딩 (37.5, 127.0)
     */
    public static MemberAddr createMainAddr(Member member) {
        return MemberAddr.builder()
                .addr1("서울특별시")
                .addr2("강남구")
                .addr3("역삼동")
                .streetAddr("테헤란로 123")
                .buildingName("ㅁㅁ빌딩")
                .latitude(37.5)
                .longitude(127.0)
                .isMain(true)
                .member(member)
                .build();
    }

    /**
     * 임의 테스트 주소
     * 비대표주소
     * - 서울특별시 서초구 서초동 서초대로 456 / ㅇㅇ빌딩 (37.4, 127.1)
     */
    public static MemberAddr createSubAddr(Member member) {
        return MemberAddr.builder()
                .addr1("서울특별시")
                .addr2("서초구")
                .addr3("서초동")
                .streetAddr("서초대로 456")
                .buildingName("ㅇㅇ빌딩")
                .latitude(37.4)
                .longitude(127.1)
                .isMain(false)
                .member(member)
                .build();
    }

    /**
     * 커스텀주소
     */
    public static MemberAddr createAddr(Member member, String addr3, String streetAddr, boolean isMain) {
        return MemberAddr.builder()
                .addr1("경기도")
                .addr2("안산시")
                .addr3(addr3)
                .streetAddr(streetAddr)
                .buildingName("빌딩" + addr3)
                .latitude(37.5)
                .longitude(127.0)
                .isMain(isMain)
                .member(member)
                .build();
    }
}
