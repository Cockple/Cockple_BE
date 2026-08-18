package umc.cockple.demo.domain.game.presentation.mapper;

import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.game.service.query.model.GameBoardMemberSearchQuery;
import umc.cockple.demo.domain.game.service.query.result.GameBoardMemberResult;
import umc.cockple.demo.domain.game.presentation.dto.GameBoardMemberDTO;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;

import java.util.List;

@Component
public class GameBoardMemberMapper {

    public GameBoardMemberSearchQuery toSearchQuery(
            List<String> levels, String gender, Boolean shuttlecockSubmitted) {
        List<Level> levelEnums = levels == null
                ? List.of()
                : levels.stream().map(Level::fromKorean).toList();
        Gender genderEnum = gender == null ? null : Gender.fromKorean(gender);
        return new GameBoardMemberSearchQuery(levelEnums, genderEnum, shuttlecockSubmitted);
    }

    public GameBoardMemberDTO.Response toResponse(GameBoardMemberResult result) {
        return new GameBoardMemberDTO.Response(
                result.totalCount(),
                result.gameBoardMembers().stream().map(this::toMemberInfo).toList());
    }

    private GameBoardMemberDTO.MemberInfo toMemberInfo(GameBoardMemberResult.MemberView member) {
        return new GameBoardMemberDTO.MemberInfo(
                member.gameBoardMemberId(),
                member.inGame(),
                member.waiting(),
                member.participating(),
                member.gameCount(),
                member.profileImageUrl(),
                member.name(),
                member.ageGroup() == null ? null : member.ageGroup().getKoreanName(),
                member.level().getKoreanName(),
                member.shuttlecockSubmitted());
    }
}
