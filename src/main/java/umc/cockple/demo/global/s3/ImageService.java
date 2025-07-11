package umc.cockple.demo.global.s3;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ImageService {

    //이미지 업로드 임시 코드
    public String uploadImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            return null;
        }

        // TODO: S3 인프라 구축 후 실제 업로드 로직으로 교체 필요
        //이미지를 보낸 경우, 실제 S3 업로드 로직 대신 하드코딩된 URL 반환
        String tempImageUrl = "https://cockple.s3.ap-northeast-2.amazonaws.com/parties/images/default-party-img.png";

        return tempImageUrl;
    }

    /**
     * 다중 이미지 업로드
     * @param images MultipartFile 이미지 리스트
     * @return 업로드된 이미지 URL 리스트
     */
    public List<String> uploadImages(List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            return List.of(); // 빈 리스트 반환
        }

        return images.stream()
                .map(this::uploadImage)
                .collect(Collectors.toList());
    }

    public String getFileKey(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            return null;
        }

        // 원본 파일명에서 확장자 추출
        String originalFilename = image.getOriginalFilename();
        String extension = StringUtils.getFilenameExtension(originalFilename);

        // UUID 기반 유니크 키 생성
        String uuid = UUID.randomUUID().toString();

        // 예: contest-images/uuid.jpg
        return "contest-images/" + uuid + "." + extension;
    }
}