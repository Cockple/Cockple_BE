package umc.cockple.demo.domain.game.presentation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import umc.cockple.demo.domain.game.presentation.controller.api.GameBoardMemberApi;
import umc.cockple.demo.domain.game.presentation.dto.GameBoardMemberDTO;
import umc.cockple.demo.domain.game.presentation.mapper.GameBoardMemberMapper;
import umc.cockple.demo.domain.game.service.query.GameBoardMemberQueryService;
import umc.cockple.demo.domain.game.service.query.model.GameBoardMemberSearchQuery;
import umc.cockple.demo.domain.game.service.query.result.GameBoardMemberResult;
import umc.cockple.demo.global.response.BaseResponse;
import umc.cockple.demo.global.response.code.status.CommonSuccessCode;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class GameBoardMemberController implements GameBoardMemberApi {

    private final GameBoardMemberQueryService gameBoardMemberQueryService;
    private final GameBoardMemberMapper gameBoardMemberMapper;

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
