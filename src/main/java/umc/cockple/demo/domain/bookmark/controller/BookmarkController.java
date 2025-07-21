package umc.cockple.demo.domain.bookmark.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import umc.cockple.demo.domain.bookmark.service.BookmarkCommandService;
import umc.cockple.demo.domain.bookmark.service.BookmarkQueryService;
import umc.cockple.demo.global.response.BaseResponse;
import umc.cockple.demo.global.response.code.status.CommonSuccessCode;

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
        // 추후 시큐리티를 통해 id 가져옴
        Long memberId = 1L;

        return BaseResponse.success(CommonSuccessCode.CREATED, bookmarkCommandService.partyBookmark(memberId, partyId));
    }

    @DeleteMapping(value = "/parties/{partyId}/bookmark")
    @Operation(summary = "모임 찜 해제 API",
            description = "사용자가 찜한 모임을 해제할 수 있음")
    public BaseResponse<Long> releasePartyBookmark(@PathVariable Long partyId) {
        // 추후 시큐리티를 통해 id 가져옴
        Long memberId = 1L;

        bookmarkCommandService.releasePartyBookmark(memberId, partyId);
        return BaseResponse.success(CommonSuccessCode.NO_CONTENT);
    }

}
