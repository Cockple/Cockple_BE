package umc.cockple.demo.domain.game.presentation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import umc.cockple.demo.domain.game.presentation.controller.api.GameApi;
import umc.cockple.demo.domain.game.presentation.dto.GameCompletedGameDTO;
import umc.cockple.demo.domain.game.presentation.dto.GameDuplicateCheckDTO;
import umc.cockple.demo.domain.game.presentation.dto.GameRandomMatchDTO;
import umc.cockple.demo.domain.game.presentation.mapper.GameCompletedGameMapper;
import umc.cockple.demo.domain.game.presentation.mapper.GameDuplicateCheckMapper;
import umc.cockple.demo.domain.game.presentation.mapper.GameRandomMatchMapper;
import umc.cockple.demo.domain.game.service.query.GameCompletedGameQueryService;
import umc.cockple.demo.domain.game.service.query.GameDuplicateCheckQueryService;
import umc.cockple.demo.domain.game.service.query.GameRandomMatchQueryService;
import umc.cockple.demo.domain.game.service.query.result.GameCompletedGameResult;
import umc.cockple.demo.domain.game.service.query.result.GameDuplicateCheckResult;
import umc.cockple.demo.domain.game.service.query.result.GameRandomMatchResult;
import umc.cockple.demo.global.response.BaseResponse;
import umc.cockple.demo.global.response.code.status.CommonSuccessCode;
import umc.cockple.demo.global.security.utils.SecurityUtil;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class GameController implements GameApi {

    private final GameDuplicateCheckQueryService gameDuplicateCheckQueryService;
    private final GameDuplicateCheckMapper gameDuplicateCheckMapper;
    private final GameCompletedGameQueryService gameCompletedGameQueryService;
    private final GameCompletedGameMapper gameCompletedGameMapper;
    private final GameRandomMatchQueryService gameRandomMatchQueryService;
    private final GameRandomMatchMapper gameRandomMatchMapper;

    @Override
    public ResponseEntity<BaseResponse<GameRandomMatchDTO.Response>> randomMatch(Long gameBoardId) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        GameRandomMatchResult result = gameRandomMatchQueryService.match(memberId, gameBoardId);

        return BaseResponse.of(CommonSuccessCode.OK, gameRandomMatchMapper.toResponse(result));
    }

    @Override
    public ResponseEntity<BaseResponse<GameDuplicateCheckDTO.Response>> checkDuplicates(
            Long gameBoardId, List<Long> gameBoardMemberIds) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        GameDuplicateCheckResult result = gameDuplicateCheckQueryService.checkDuplicates(
                memberId, gameBoardId, gameBoardMemberIds);

        return BaseResponse.of(CommonSuccessCode.OK, gameDuplicateCheckMapper.toResponse(result));
    }

    @Override
    public ResponseEntity<BaseResponse<GameCompletedGameDTO.Response>> getCompletedGames(
            Long gameBoardId, Integer courtNo, String cursor, int size) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        GameCompletedGameResult result = gameCompletedGameQueryService.getCompletedGames(
                memberId, gameBoardId, courtNo, cursor, size);

        return BaseResponse.of(CommonSuccessCode.OK, gameCompletedGameMapper.toResponse(result));
    }
}
