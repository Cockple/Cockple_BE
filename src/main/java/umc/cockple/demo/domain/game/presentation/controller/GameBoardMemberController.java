package umc.cockple.demo.domain.game.presentation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import umc.cockple.demo.domain.game.presentation.controller.api.GameBoardMemberApi;
import umc.cockple.demo.domain.game.presentation.dto.GameBoardMemberDTO;
import umc.cockple.demo.domain.game.presentation.mapper.GameBoardMemberMapper;
import umc.cockple.demo.domain.game.service.command.GameBoardMemberCommandService;
import umc.cockple.demo.domain.game.service.command.model.GameBoardMemberCreateCommand;
import umc.cockple.demo.domain.game.service.command.model.GameBoardMemberUpdateCommand;
import umc.cockple.demo.domain.game.service.query.GameBoardMemberQueryService;
import umc.cockple.demo.domain.game.service.query.model.GameBoardMemberSearchQuery;
import umc.cockple.demo.domain.game.service.query.result.GameBoardMemberResult;
import umc.cockple.demo.global.response.BaseResponse;
import umc.cockple.demo.global.response.code.status.CommonSuccessCode;
import umc.cockple.demo.global.security.utils.SecurityUtil;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class GameBoardMemberController implements GameBoardMemberApi {

    private final GameBoardMemberCommandService gameBoardMemberCommandService;
    private final GameBoardMemberQueryService gameBoardMemberQueryService;
    private final GameBoardMemberMapper gameBoardMemberMapper;

    @Override
    public ResponseEntity<BaseResponse<Void>> updateMember(
            Long gameBoardId,
            Long gameBoardMemberId,
            GameBoardMemberDTO.UpdateRequest request) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        GameBoardMemberUpdateCommand command = gameBoardMemberMapper.toUpdateCommand(
                gameBoardId, gameBoardMemberId, request);

        gameBoardMemberCommandService.updateMember(memberId, command);

        return BaseResponse.of(CommonSuccessCode.OK);
    }

    @Override
    public ResponseEntity<BaseResponse<GameBoardMemberDTO.CreateResponse>> createMember(
            Long gameBoardId, GameBoardMemberDTO.CreateRequest request) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        GameBoardMemberCreateCommand command = gameBoardMemberMapper.toCreateCommand(gameBoardId, request);

        Long gameBoardMemberId = gameBoardMemberCommandService.createMember(memberId, command);

        return BaseResponse.of(
                CommonSuccessCode.OK,
                gameBoardMemberMapper.toCreateResponse(gameBoardMemberId));
    }

    @Override
    public ResponseEntity<BaseResponse<GameBoardMemberDTO.Response>> getMembers(
            Long gameBoardId, List<String> levels, String gender, Boolean shuttlecockSubmitted) {
        GameBoardMemberSearchQuery searchQuery = gameBoardMemberMapper.toSearchQuery(
                levels, gender, shuttlecockSubmitted);

        GameBoardMemberResult result = gameBoardMemberQueryService.getMembers(
                gameBoardId, searchQuery);

        return BaseResponse.of(CommonSuccessCode.OK, gameBoardMemberMapper.toResponse(result));
    }
}
