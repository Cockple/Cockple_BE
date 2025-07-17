package umc.cockple.demo.domain.member.converter;

import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.dto.GetProfileResponseDTO;

public class MemberConverter {

    public static GetProfileResponseDTO memberToGetProfileResponseDTO(Member member, int goldMedalCnt,
                                                                      int silverMedalCnt, int bronzeMedalCnt, String imgUrl) {
        return GetProfileResponseDTO.builder()
                .memberName(member.getMemberName())
                .birth(member.getBirth())
                .gender(member.getGender())
                .level(member.getLevel())
                .profileImgUrl(imgUrl)
                .myPartyCnt(member.getMemberParties().size())
                .myGoldMedalCnt(goldMedalCnt)
                .mySilverMedalCnt(silverMedalCnt)
                .myBronzeMedalCnt(bronzeMedalCnt)
                .build();
    }
}
