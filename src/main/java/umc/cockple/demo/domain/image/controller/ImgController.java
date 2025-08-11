package umc.cockple.demo.domain.image.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import umc.cockple.demo.domain.image.dto.FileUploadDTO;
import umc.cockple.demo.domain.image.dto.ImageUploadDTO;
import umc.cockple.demo.domain.image.service.ImageService;
import umc.cockple.demo.global.enums.DomainType;
import umc.cockple.demo.global.response.BaseResponse;
import umc.cockple.demo.global.response.code.status.CommonSuccessCode;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
@Tag(name = "Image", description = "이미지 API")
public class ImgController {

    private final ImageService imageService;

    @PostMapping(value = "/s3/upload/img", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "이미지 업로드", description = "S3에 이미지를 업로드하고 이미지 URL과 imgKey를 반환합니다.")
    public BaseResponse<ImageUploadDTO.Response> imgUpload(@RequestPart("image") MultipartFile image,
                                                           @RequestParam("domainType") DomainType domainType) {

        return BaseResponse.success(CommonSuccessCode.ACCEPTED, imageService.uploadImage(image, domainType));
    }


    @PostMapping(value = "/s3/upload/imgs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "이미지 여러장 업로드", description = "S3에 이미지 여러장을 업로드하고 이미지 URL과 imgKey를 반환합니다.")
    public BaseResponse<List<ImageUploadDTO.Response>> imgUpload(@RequestPart("image") List<MultipartFile> images,
                                                                @RequestParam("domainType") DomainType domainType) {

        return BaseResponse.success(CommonSuccessCode.ACCEPTED, imageService.uploadImages(images, domainType));
    }

    @PostMapping(value = "/s3/upload/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "파일 업로드", description = "S3에 파일을 업로드하고 파일정보를 반환합니다.")
    public BaseResponse<FileUploadDTO.Response> fileUpload(@RequestPart("file") MultipartFile file,
                                                  @RequestParam("domainType") DomainType domainType) {

        return BaseResponse.success(CommonSuccessCode.ACCEPTED, imageService.uploadFile(file, domainType));
    }
}
