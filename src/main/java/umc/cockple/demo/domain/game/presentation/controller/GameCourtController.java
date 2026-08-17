package umc.cockple.demo.domain.game.presentation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import umc.cockple.demo.domain.game.presentation.controller.api.GameCourtApi;
import umc.cockple.demo.domain.game.presentation.dto.GameCourtDTO;
import umc.cockple.demo.domain.game.presentation.mapper.GameCourtMapper;
import umc.cockple.demo.domain.game.service.command.GameCourtCommandService;
import umc.cockple.demo.domain.game.service.command.model.GameCourtManageCommand;
import umc.cockple.demo.domain.game.service.command.result.GameCourtManageResult;
import umc.cockple.demo.global.response.BaseResponse;
import umc.cockple.demo.global.response.code.status.CommonSuccessCode;
import umc.cockple.demo.global.security.utils.SecurityUtil;

@RestController
@RequiredArgsConstructor
@Validated
public class GameCourtController implements GameCourtApi {

    private final GameCourtCommandService gameCourtCommandService;
    private final GameCourtMapper gameCourtMapper;

    @Override
    public ResponseEntity<BaseResponse<GameCourtDTO.Response>> manageCourts(
            Long gameBoardId, GameCourtDTO.Request request) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        GameCourtManageCommand command = gameCourtMapper.toManageCommand(gameBoardId, request);
        GameCourtManageResult result = gameCourtCommandService.manageCourts(memberId, command);

        return BaseResponse.of(CommonSuccessCode.OK, gameCourtMapper.toResponse(result));
    }
}
