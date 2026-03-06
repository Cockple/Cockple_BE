package umc.cockple.demo.domain.file.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import umc.cockple.demo.domain.file.dto.FileUploadDTO;
import umc.cockple.demo.domain.file.service.FileService;
import umc.cockple.demo.global.enums.DomainType;
import umc.cockple.demo.global.response.BaseResponse;
import umc.cockple.demo.global.response.code.status.CommonSuccessCode;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
@Tag(name = "File", description = "파일 업로드 API")
public class FileController {

    private final FileService fileService;

    @PostMapping(value = "/s3/upload/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "파일 업로드", description = "GCS에 파일을 업로드하고 파일 URL과 fileKey를 반환합니다.")
    public BaseResponse<FileUploadDTO.Response> fileUpload(@RequestPart("file") MultipartFile file,
                                                           @RequestParam("domainType") DomainType domainType) {

        return BaseResponse.success(CommonSuccessCode.ACCEPTED, fileService.uploadFile(file, domainType));
    }

    @PostMapping(value = "/s3/upload/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "파일 여러장 업로드", description = "GCS에 파일 여러장을 업로드하고 파일 URL과 fileKey를 반환합니다.")
    public BaseResponse<List<FileUploadDTO.Response>> fileUpload(@RequestPart("file") List<MultipartFile> files,
                                                                 @RequestParam("domainType") DomainType domainType) {

        return BaseResponse.success(CommonSuccessCode.ACCEPTED, fileService.uploadFiles(files, domainType));
    }
}
