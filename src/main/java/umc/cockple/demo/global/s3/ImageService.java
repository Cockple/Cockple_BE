package umc.cockple.demo.global.s3;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
}