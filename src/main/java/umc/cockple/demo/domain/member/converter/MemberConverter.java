package umc.cockple.demo.domain.member.converter;

import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberAddr;
import umc.cockple.demo.domain.member.dto.GetMyProfileResponseDTO;
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

    public static GetMyProfileResponseDTO toGetMyProfileResponseDTO(GetProfileResponseDTO dto, MemberAddr mainAddr, Integer exerciseCnt) {
        return GetMyProfileResponseDTO.builder()
                .memberName(dto.memberName())
                .birth(dto.birth())
                .gender(dto.gender())
                .level(dto.level())
                .addr3(mainAddr.getAddr3())
                .latitude(mainAddr.getLatitude())
                .longitude(mainAddr.getLongitude())
                .profileImgUrl(dto.profileImgUrl())
                .myPartyCnt(dto.myPartyCnt())
                .myExerciseCnt(exerciseCnt)
                .myGoldMedalCnt(dto.myGoldMedalCnt())
                .mySilverMedalCnt(dto.mySilverMedalCnt())
                .myBronzeMedalCnt(dto.myBronzeMedalCnt())
                .build();
    }
}
