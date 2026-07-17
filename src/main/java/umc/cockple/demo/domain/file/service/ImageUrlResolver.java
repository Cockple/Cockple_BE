package umc.cockple.demo.domain.file.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class ImageUrlResolver {

    private final FileService fileService;

    public <T> String resolve(T image, Function<T, String> imageKeyExtractor) {
        if (image == null) {
            return null;
        }

        return resolve(imageKeyExtractor.apply(image));
    }

    public String resolve(String imageKey) {
        if (!StringUtils.hasText(imageKey)) {
            return null;
        }

        return fileService.getUrlFromKey(imageKey);
    }
}
