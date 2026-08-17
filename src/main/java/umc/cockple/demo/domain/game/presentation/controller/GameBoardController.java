package umc.cockple.demo.domain.game.presentation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import umc.cockple.demo.domain.game.presentation.controller.api.GameBoardApi;
import umc.cockple.demo.domain.game.presentation.dto.GameBoardDTO;
import umc.cockple.demo.domain.game.presentation.mapper.GameBoardMapper;
import umc.cockple.demo.domain.game.service.query.GameBoardQueryService;
import umc.cockple.demo.domain.game.service.query.result.GameBoardResult;
import umc.cockple.demo.global.response.BaseResponse;
import umc.cockple.demo.global.response.code.status.CommonSuccessCode;
import umc.cockple.demo.global.security.utils.SecurityUtil;

@RestController
@RequiredArgsConstructor
public class GameBoardController implements GameBoardApi {

    private final GameBoardQueryService gameBoardQueryService;
    private final GameBoardMapper gameBoardMapper;

    @Override
    public ResponseEntity<BaseResponse<GameBoardDTO.Response>> getBoard(Long gameBoardId) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        GameBoardResult result = gameBoardQueryService.getBoard(memberId, gameBoardId);

        return BaseResponse.of(CommonSuccessCode.OK, gameBoardMapper.toResponse(result));
    }
}
