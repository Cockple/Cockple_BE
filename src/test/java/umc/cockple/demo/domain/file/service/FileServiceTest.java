package umc.cockple.demo.domain.file.service;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import umc.cockple.demo.domain.file.dto.FileUploadDTO;
import umc.cockple.demo.domain.file.exception.GcsErrorCode;
import umc.cockple.demo.domain.file.exception.GcsException;
import umc.cockple.demo.global.enums.DomainType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileService 단위 테스트")
class FileServiceTest {

    @Mock
    private Storage storage;

    private FileService fileService;

    private String bucketName = "test-bucket";
    private MockMultipartFile mockFile;

    @BeforeEach
    void setUp() {
        fileService = new FileService(storage);
        ReflectionTestUtils.setField(fileService, "bucket", bucketName);
        mockFile = new MockMultipartFile("file", "test.jpg", MediaType.IMAGE_JPEG_VALUE, "test image content".getBytes());
    }

    // ========== uploadFile ==========
    @Nested
    @DisplayName("uploadFile - 단일 파일 업로드 테스트")
    class UploadFile {

        @Test
        @DisplayName("정상적인 MultipartFile이 주어지면 GCS에 업로드하고 응답 DTO를 반환한다")
        void success() {
            given(storage.create(any(BlobInfo.class), any(byte[].class))).willReturn(mock(Blob.class));

            FileUploadDTO.Response response = fileService.uploadFile(mockFile, DomainType.CHAT);

            assertThat(response).isNotNull();
            assertThat(response.fileKey()).startsWith("chat/");
            assertThat(response.fileKey()).endsWith(".jpg");
            assertThat(response.fileUrl()).isEqualTo("https://storage.googleapis.com/" + bucketName + "/" + response.fileKey());
            assertThat(response.originalFileName()).isEqualTo("test.jpg");
            verify(storage).create(any(BlobInfo.class), any(byte[].class));
        }

        @Test
        @DisplayName("파일이 비어있으면 null을 반환한다")
        void returnNullWhenFileIsEmpty() {
            MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);

            FileUploadDTO.Response response = fileService.uploadFile(emptyFile, DomainType.CHAT);

            assertThat(response).isNull();
            verify(storage, never()).create(any(BlobInfo.class), any(byte[].class));
        }

        @Test
        @DisplayName("StorageException 발생 시 FILE_UPLOAD_GCS_EXCEPTION이 발생한다")
        void throwExceptionWhenGcsError() {
            given(storage.create(any(BlobInfo.class), any(byte[].class))).willThrow(new StorageException(500, "GCS 에러"));

            GcsException exception = assertThrows(GcsException.class, () -> fileService.uploadFile(mockFile, DomainType.CHAT));

            assertThat(exception.getCode()).isEqualTo(GcsErrorCode.FILE_UPLOAD_GCS_EXCEPTION);
        }
    }

    // ========== uploadFiles ==========
    @Nested
    @DisplayName("uploadFiles - 다중 파일 업로드 테스트")
    class UploadFiles {

        @Test
        @DisplayName("정상적인 파일 목록이 주어지면 업로드된 응답 DTO 리스트를 반환한다")
        void success() {
            MockMultipartFile mockFile2 = new MockMultipartFile("file", "test2.png", MediaType.IMAGE_PNG_VALUE, "content2".getBytes());
            given(storage.create(any(BlobInfo.class), any(byte[].class))).willReturn(mock(Blob.class));

            List<FileUploadDTO.Response> responses = fileService.uploadFiles(List.of(mockFile, mockFile2), DomainType.PARTY);

            assertThat(responses).hasSize(2);
            assertThat(responses.get(0).originalFileName()).isEqualTo("test.jpg");
            assertThat(responses.get(1).originalFileName()).isEqualTo("test2.png");
            // 2번 호출됐는지 확인
            verify(storage, times(2)).create(any(BlobInfo.class), any(byte[].class));
        }

        @Test
        @DisplayName("파일 리스트가 null이거나 비어있으면 빈 리스트를 반환한다")
        void returnEmptyListWhenFilesAreEmpty() {
            List<FileUploadDTO.Response> responses = fileService.uploadFiles(List.of(), DomainType.PARTY);

            assertThat(responses).isEmpty();
            verify(storage, never()).create(any(BlobInfo.class), any(byte[].class));
        }
    }

    // ========== delete ==========
    @Nested
    @DisplayName("delete - 파일 삭제 테스트")
    class Delete {

        @Test
        @DisplayName("유효한 fileKey가 주어지면 정상적으로 삭제를 수행한다")
        void success() {
            String fileKey = "chat/uuid-1234.jpg";
            BlobId expectedBlobId = BlobId.of(bucketName, fileKey);
            given(storage.delete(expectedBlobId)).willReturn(true);

            fileService.delete(fileKey);

            verify(storage).delete(expectedBlobId);
        }
    }

    // ========== downloadFile ==========
    @Nested
    @DisplayName("downloadFile - 파일 다운로드(Blob 조회) 테스트")
    class DownloadFile {

        @Test
        @DisplayName("유효한 fileKey를 주면 GCS에서 Blob 객체를 반환한다")
        void success() {
            String fileKey = "chat/valid-key.jpg";
            Blob mockBlob = mock(Blob.class);
            given(storage.get(BlobId.of(bucketName, fileKey))).willReturn(mockBlob);

            Blob result = fileService.downloadFile(fileKey);

            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(mockBlob);
        }

        @Test
        @DisplayName("Blob이 존재하지 않으면 FILE_DELETE_EXCEPTION 예외가 발생한다")
        void throwExceptionWhenBlobIsNull() {
            String fileKey = "chat/not-found.jpg";
            given(storage.get(BlobId.of(bucketName, fileKey))).willReturn(null);

            GcsException exception = assertThrows(GcsException.class, () ->
                    fileService.downloadFile(fileKey)
            );

            assertThat(exception.getCode()).isEqualTo(GcsErrorCode.FILE_DELETE_EXCEPTION);
        }
    }
}
