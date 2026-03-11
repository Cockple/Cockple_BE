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

    /**
     * 서울특별시 강남구 역삼동 (AddMemberNewAddr requestDto 기본값과 동일 - 중복 검증 등에 활용)
     * - 서울특별시 강남구 역삼동 테헤란로 123 / 테스트빌딩 (37.5, 127.0)
     */
    public static MemberAddr createSeoulAddr(Member member, boolean isMain) {
        return MemberAddr.builder()
                .addr1("서울특별시")
                .addr2("강남구")
                .addr3("역삼동")
                .streetAddr("테헤란로 123")
                .buildingName("테스트빌딩")
                .latitude(37.5)
                .longitude(127.0)
                .isMain(isMain)
                .member(member)
                .build();
    }

    /**
     * 부산광역시 해운대구 좌동 (대표주소 해제 등 기존 주소 대체용)
     * - 부산광역시 해운대구 좌동 해운대로 123 / 해운대빌딩 (35.1, 129.1)
     */
    public static MemberAddr createBusanAddr(Member member, boolean isMain) {
        return MemberAddr.builder()
                .addr1("부산광역시")
                .addr2("해운대구")
                .addr3("좌동")
                .streetAddr("해운대로 123")
                .buildingName("해운대빌딩")
                .latitude(35.1)
                .longitude(129.1)
                .isMain(isMain)
                .member(member)
                .build();
    }

    /**
     * 주소 5개 초과 테스트용 - index로 구분되는 고유 주소 생성
     */
    public static MemberAddr createAddrWithIndex(Member member, int index, boolean isMain) {
        return MemberAddr.builder()
                .addr1("서울특별시")
                .addr2("구" + index)
                .addr3("동" + index)
                .streetAddr("도로" + index)
                .buildingName("빌딩" + index)
                .latitude(37.5 + index)
                .longitude(127.0 + index)
                .isMain(isMain)
                .member(member)
                .build();
    }
}
