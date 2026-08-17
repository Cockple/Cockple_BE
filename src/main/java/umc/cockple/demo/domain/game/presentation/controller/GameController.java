package umc.cockple.demo.domain.game.presentation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import umc.cockple.demo.domain.game.presentation.controller.api.GameApi;
import umc.cockple.demo.domain.game.presentation.dto.GameDuplicateCheckDTO;
import umc.cockple.demo.domain.game.presentation.mapper.GameDuplicateCheckMapper;
import umc.cockple.demo.domain.game.service.query.GameDuplicateCheckQueryService;
import umc.cockple.demo.domain.game.service.query.result.GameDuplicateCheckResult;
import umc.cockple.demo.global.response.BaseResponse;
import umc.cockple.demo.global.response.code.status.CommonSuccessCode;
import umc.cockple.demo.global.security.utils.SecurityUtil;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class GameController implements GameApi {

    private final GameDuplicateCheckQueryService gameDuplicateCheckQueryService;
    private final GameDuplicateCheckMapper gameDuplicateCheckMapper;

    @Override
    public ResponseEntity<BaseResponse<GameDuplicateCheckDTO.Response>> checkDuplicates(
            Long gameBoardId, List<Long> gameBoardMemberIds) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        GameDuplicateCheckResult result = gameDuplicateCheckQueryService.checkDuplicates(
                memberId, gameBoardId, gameBoardMemberIds);

        return BaseResponse.of(CommonSuccessCode.OK, gameDuplicateCheckMapper.toResponse(result));
    }
}
