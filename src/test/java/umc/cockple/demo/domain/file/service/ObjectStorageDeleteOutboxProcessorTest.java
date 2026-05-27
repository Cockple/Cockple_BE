package umc.cockple.demo.domain.file.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import umc.cockple.demo.domain.file.domain.ObjectStorageDeleteOutbox;
import umc.cockple.demo.domain.file.enums.ObjectStorageDeleteSourceType;
import umc.cockple.demo.domain.file.enums.ObjectStorageDeleteStatus;
import umc.cockple.demo.domain.file.repository.ObjectStorageDeleteOutboxRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ObjectStorageDeleteOutboxProcessor 단위 테스트")
class ObjectStorageDeleteOutboxProcessorTest {

    @InjectMocks
    private ObjectStorageDeleteOutboxProcessor objectStorageDeleteOutboxProcessor;

    @Mock
    private ObjectStorageDeleteOutboxRepository objectStorageDeleteOutboxRepository;
    @Mock
    private ObjectStorageClient objectStorageClient;

    @Test
    @DisplayName("PENDING/FAILED outbox를 batchSize만큼 조회해 처리한다")
    void processPendingBatch_deletesRetryTargets() {
        ReflectionTestUtils.setField(objectStorageDeleteOutboxProcessor, "batchSize", 2);
        ReflectionTestUtils.setField(objectStorageDeleteOutboxProcessor, "maxRetryCount", 5);
        ObjectStorageDeleteOutbox first = ObjectStorageDeleteOutbox.pending(
                "chat/first.jpg",
                ObjectStorageDeleteSourceType.PARTY_CHAT_ROOM,
                10L
        );
        ObjectStorageDeleteOutbox second = ObjectStorageDeleteOutbox.pending(
                "chat/second.jpg",
                ObjectStorageDeleteSourceType.PARTY_CHAT_ROOM,
                10L
        );
        given(objectStorageDeleteOutboxRepository.findByStatusInAndRetryCountLessThanOrderByCreatedAtAsc(
                List.of(ObjectStorageDeleteStatus.PENDING, ObjectStorageDeleteStatus.FAILED),
                5,
                PageRequest.of(0, 2)
        )).willReturn(List.of(first, second));

        int processedCount = objectStorageDeleteOutboxProcessor.processPendingBatch();

        assertThat(processedCount).isEqualTo(2);
        verify(objectStorageClient).delete("chat/first.jpg");
        verify(objectStorageClient).delete("chat/second.jpg");
        assertThat(first.getStatus()).isEqualTo(ObjectStorageDeleteStatus.DONE);
        assertThat(second.getStatus()).isEqualTo(ObjectStorageDeleteStatus.DONE);
    }

    @Test
    @DisplayName("삭제에 성공하면 outbox를 DONE으로 표시한다")
    void processOne_marksDoneWhenDeleteSucceeds() {
        ObjectStorageDeleteOutbox outbox = ObjectStorageDeleteOutbox.pending(
                "chat/file.jpg",
                ObjectStorageDeleteSourceType.PARTY_CHAT_ROOM,
                10L
        );
        given(objectStorageDeleteOutboxRepository.findById(1L)).willReturn(Optional.of(outbox));

        objectStorageDeleteOutboxProcessor.processOne(1L);

        verify(objectStorageClient).delete("chat/file.jpg");
        assertThat(outbox.getStatus()).isEqualTo(ObjectStorageDeleteStatus.DONE);
        assertThat(outbox.getLastError()).isNull();
        assertThat(outbox.getLastAttemptedAt()).isNotNull();
    }

    @Test
    @DisplayName("삭제에 실패하면 실패 사유와 retryCount를 남긴다")
    void processOne_marksFailedWhenDeleteFails() {
        ObjectStorageDeleteOutbox outbox = ObjectStorageDeleteOutbox.pending(
                "chat/file.jpg",
                ObjectStorageDeleteSourceType.PARTY_CHAT_ROOM,
                10L
        );
        given(objectStorageDeleteOutboxRepository.findById(1L)).willReturn(Optional.of(outbox));
        willThrow(new RuntimeException("delete failed"))
                .given(objectStorageClient)
                .delete("chat/file.jpg");

        objectStorageDeleteOutboxProcessor.processOne(1L);

        assertThat(outbox.getStatus()).isEqualTo(ObjectStorageDeleteStatus.FAILED);
        assertThat(outbox.getRetryCount()).isEqualTo(1);
        assertThat(outbox.getLastError()).isEqualTo("delete failed");
        assertThat(outbox.getLastAttemptedAt()).isNotNull();
    }
}
