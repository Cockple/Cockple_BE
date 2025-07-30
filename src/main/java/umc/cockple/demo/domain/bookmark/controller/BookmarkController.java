package umc.cockple.demo.domain.bookmark.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import umc.cockple.demo.domain.bookmark.dto.GetAllExerciseBookmarksResponseDTO;
import umc.cockple.demo.domain.bookmark.dto.GetAllPartyBookmarkResponseDTO;
import umc.cockple.demo.domain.bookmark.service.BookmarkCommandService;
import umc.cockple.demo.domain.bookmark.service.BookmarkQueryService;
import umc.cockple.demo.global.enums.ExerciseOrderType;
import umc.cockple.demo.global.enums.PartyOrderType;
import umc.cockple.demo.global.response.BaseResponse;
import umc.cockple.demo.global.response.code.status.CommonSuccessCode;
import umc.cockple.demo.global.security.utils.SecurityUtil;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
@Tag(name = "Bookmark", description = "찜 API")
public class BookmarkController {

    private final BookmarkCommandService bookmarkCommandService;
    private final BookmarkQueryService bookmarkQueryService;

    @PostMapping(value = "/parties/{partyId}/bookmark")
    @Operation(summary = "모임 찜 API",
            description = "사용자가 원하는 모임을 찜해둘 수 있음")
    public BaseResponse<Long> partyBookmark(@PathVariable Long partyId) {

        Long memberId = SecurityUtil.getCurrentMemberId();

        return BaseResponse.success(CommonSuccessCode.CREATED, bookmarkCommandService.partyBookmark(memberId, partyId));
    }

    @DeleteMapping(value = "/parties/{partyId}/bookmark")
    @Operation(summary = "모임 찜 해제 API",
            description = "사용자가 찜한 모임을 해제할 수 있음")
    public BaseResponse<Object> releasePartyBookmark(@PathVariable Long partyId) {

        Long memberId = SecurityUtil.getCurrentMemberId();

        bookmarkCommandService.releasePartyBookmark(memberId, partyId);
        return BaseResponse.success(CommonSuccessCode.NO_CONTENT);
    }

    @PostMapping(value = "/exercises/{exerciseId}/bookmark")
    @Operation(summary = "운동 찜 API",
            description = "사용자가 원하는 운동을 찜해둘 수 있음")
    public BaseResponse<Long> exerciseBookmark(@PathVariable Long exerciseId) {

        Long memberId = SecurityUtil.getCurrentMemberId();

        return BaseResponse.success(CommonSuccessCode.CREATED, bookmarkCommandService.exerciseBookmark(memberId, exerciseId));
    }

    @DeleteMapping(value = "/exercises/{exerciseId}/bookmark")
    @Operation(summary = "운동 찜 해제 API",
            description = "사용자가 찜한 운동 해제할 수 있음")
    public BaseResponse<Object> releaseExerciseBookmark(@PathVariable Long exerciseId) {

        Long memberId = SecurityUtil.getCurrentMemberId();

        bookmarkCommandService.releaseExerciseBookmark(memberId, exerciseId);
        return BaseResponse.success(CommonSuccessCode.NO_CONTENT);
    }


    @GetMapping(value = "/exercises/bookmarks")
    @Operation(summary = "찜한 운동 전체 조회 API",
            description = "사용자가 찜한 운동을 모두 조회")
    public BaseResponse<List<GetAllExerciseBookmarksResponseDTO>> getAllExerciseBookmarks(@RequestParam ExerciseOrderType orderType) {

        Long memberId = SecurityUtil.getCurrentMemberId();

        return BaseResponse.success(CommonSuccessCode.OK, bookmarkQueryService.getAllExerciseBookmarks(memberId, orderType));
    }

    @GetMapping(value = "/parties/bookmarks")
    @Operation(summary = "찜한 모임 전체 조회 API",
            description = "사용자가 찜한 모임을 모두 조회")
    public BaseResponse<List<GetAllPartyBookmarkResponseDTO>> getAllPartyBookmarks(@RequestParam PartyOrderType orderType) {

        Long memberId = SecurityUtil.getCurrentMemberId();

        return BaseResponse.success(CommonSuccessCode.OK, bookmarkQueryService.getAllPartyBookmarks(memberId, orderType));
    }


}
