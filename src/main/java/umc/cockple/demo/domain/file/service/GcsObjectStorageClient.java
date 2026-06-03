package umc.cockple.demo.domain.file.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GcsObjectStorageClient implements ObjectStorageClient {

    private final FileService fileService;

    @Override
    public void delete(String objectKey) {
        fileService.delete(objectKey);
    }
}
