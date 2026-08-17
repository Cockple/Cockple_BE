package umc.cockple.demo.domain.exercise.presentation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import umc.cockple.demo.domain.exercise.presentation.controller.api.ExerciseGameHostApi;
import umc.cockple.demo.domain.exercise.presentation.dto.gamehost.ExerciseGameHostDTO;
import umc.cockple.demo.domain.exercise.presentation.mapper.query.ExerciseGameHostQueryMapper;
import umc.cockple.demo.domain.exercise.service.query.ExerciseGameHostQueryService;
import umc.cockple.demo.domain.exercise.service.query.result.ExerciseGameHostResult;
import umc.cockple.demo.global.response.BaseResponse;
import umc.cockple.demo.global.response.code.status.CommonSuccessCode;
import umc.cockple.demo.global.security.utils.SecurityUtil;

@RestController
@RequiredArgsConstructor
public class ExerciseGameHostController implements ExerciseGameHostApi {

    private final ExerciseGameHostQueryService exerciseGameHostQueryService;
    private final ExerciseGameHostQueryMapper exerciseGameHostQueryMapper;

    @Override
    public ResponseEntity<BaseResponse<ExerciseGameHostDTO.Response>> getGameHost(Long exerciseId) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        ExerciseGameHostResult result = exerciseGameHostQueryService.getGameHost(exerciseId, memberId);
        ExerciseGameHostDTO.Response response = exerciseGameHostQueryMapper.toResponse(result);

        return BaseResponse.of(CommonSuccessCode.OK, response);
    }
}
