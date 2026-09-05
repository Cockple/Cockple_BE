package umc.cockple.demo.domain.game.presentation.mapper;

import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.game.enums.AgeGroup;
import umc.cockple.demo.domain.game.presentation.dto.GameBoardMemberDTO;
import umc.cockple.demo.domain.game.service.command.model.GameBoardMemberCreateCommand;
import umc.cockple.demo.domain.game.service.command.model.GameBoardMemberParticipationCommand;
import umc.cockple.demo.domain.game.service.command.model.GameBoardMemberShuttlecockSubmissionCommand;
import umc.cockple.demo.domain.game.service.command.model.GameBoardMemberUpdateCommand;
import umc.cockple.demo.domain.game.service.query.model.GameBoardMemberSearchQuery;
import umc.cockple.demo.domain.game.service.query.result.GameBoardMemberResult;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;

import java.util.List;

@Component
public class GameBoardMemberMapper {

    public GameBoardMemberCreateCommand toCreateCommand(
            Long gameBoardId, GameBoardMemberDTO.CreateRequest request) {
        AgeGroup ageGroup = request.ageGroup() == null
                ? null
                : AgeGroup.fromKorean(request.ageGroup());
        return new GameBoardMemberCreateCommand(
                gameBoardId,
                request.name().trim(),
                Gender.fromKorean(request.gender()),
                Level.fromKorean(request.level()),
                ageGroup);
    }

    public GameBoardMemberDTO.CreateResponse toCreateResponse(Long gameBoardMemberId) {
        return new GameBoardMemberDTO.CreateResponse(gameBoardMemberId);
    }

    public GameBoardMemberUpdateCommand toUpdateCommand(
            Long gameBoardId,
            Long gameBoardMemberId,
            GameBoardMemberDTO.UpdateRequest request) {
        AgeGroup ageGroup = request.ageGroup() == null
                ? null
                : AgeGroup.fromKorean(request.ageGroup());
        return new GameBoardMemberUpdateCommand(
                gameBoardId,
                gameBoardMemberId,
                request.name().trim(),
                Gender.fromKorean(request.gender()),
                Level.fromKorean(request.level()),
                ageGroup);
    }

    public GameBoardMemberParticipationCommand toParticipationCommand(
            Long gameBoardId,
            Long gameBoardMemberId,
            GameBoardMemberDTO.ParticipationRequest request) {
        return new GameBoardMemberParticipationCommand(
                gameBoardId, gameBoardMemberId, request.participating());
    }

    public GameBoardMemberShuttlecockSubmissionCommand toShuttlecockSubmissionCommand(
            Long gameBoardId,
            Long gameBoardMemberId,
            GameBoardMemberDTO.ShuttlecockSubmissionRequest request) {
        return new GameBoardMemberShuttlecockSubmissionCommand(
                gameBoardId, gameBoardMemberId, request.shuttlecockSubmitted());
    }

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
                member.gender().getKoreanName(),
                member.ageGroup() == null ? null : member.ageGroup().getKoreanName(),
                member.level().getKoreanName(),
                member.shuttlecockSubmitted());
    }
}
