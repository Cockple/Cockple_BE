package umc.cockple.demo.domain.file.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("GcsObjectStorageClient 단위 테스트")
class GcsObjectStorageClientTest {

    @InjectMocks
    private GcsObjectStorageClient gcsObjectStorageClient;

    @Mock
    private FileService fileService;

    @Test
    @DisplayName("objectKey 삭제를 FileService에 위임한다")
    void delete_delegatesToFileService() {
        gcsObjectStorageClient.delete("chat/file.jpg");

        verify(fileService).delete("chat/file.jpg");
    }
}
