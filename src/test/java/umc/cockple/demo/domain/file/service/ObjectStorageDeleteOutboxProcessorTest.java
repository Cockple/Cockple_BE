package umc.cockple.demo.domain.file.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import umc.cockple.demo.domain.file.enums.ObjectStorageDeleteStatus;
import umc.cockple.demo.domain.file.repository.ObjectStorageDeleteOutboxRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ObjectStorageDeleteOutboxProcessor 단위 테스트")
class ObjectStorageDeleteOutboxProcessorTest {

    @InjectMocks
    private ObjectStorageDeleteOutboxProcessor objectStorageDeleteOutboxProcessor;

    @Mock
    private ObjectStorageDeleteOutboxRepository objectStorageDeleteOutboxRepository;
    @Mock
    private ObjectStorageDeleteOutboxClaimService objectStorageDeleteOutboxClaimService;
    @Mock
    private ObjectStorageClient objectStorageClient;

    @Test
    @DisplayName("PENDING/FAILED outbox를 batchSize만큼 조회해 처리한다")
    void processPendingBatch_deletesRetryTargets() {
        ReflectionTestUtils.setField(objectStorageDeleteOutboxProcessor, "batchSize", 2);
        ReflectionTestUtils.setField(objectStorageDeleteOutboxProcessor, "maxRetryCount", 5);
        ReflectionTestUtils.setField(objectStorageDeleteOutboxProcessor, "processingTimeoutMinutes", 10L);
        ClaimedObjectStorageDeleteOutbox first = new ClaimedObjectStorageDeleteOutbox(1L, "chat/first.jpg", "token-1");
        ClaimedObjectStorageDeleteOutbox second = new ClaimedObjectStorageDeleteOutbox(2L, "chat/second.jpg", "token-2");
        given(objectStorageDeleteOutboxRepository.findClaimCandidateIds(
                eq(List.of(ObjectStorageDeleteStatus.PENDING, ObjectStorageDeleteStatus.FAILED)),
                eq(ObjectStorageDeleteStatus.PROCESSING),
                eq(5),
                any(LocalDateTime.class),
                eq(PageRequest.of(0, 2))
        )).willReturn(List.of(1L, 2L));
        given(objectStorageDeleteOutboxClaimService.claim(eq(1L), eq(5), any(LocalDateTime.class)))
                .willReturn(Optional.of(first));
        given(objectStorageDeleteOutboxClaimService.claim(eq(2L), eq(5), any(LocalDateTime.class)))
                .willReturn(Optional.of(second));
        given(objectStorageDeleteOutboxClaimService.markDone(first)).willReturn(true);
        given(objectStorageDeleteOutboxClaimService.markDone(second)).willReturn(true);

        int processedCount = objectStorageDeleteOutboxProcessor.processPendingBatch();

        assertThat(processedCount).isEqualTo(2);
        verify(objectStorageClient).delete("chat/first.jpg");
        verify(objectStorageClient).delete("chat/second.jpg");
        verify(objectStorageDeleteOutboxClaimService).markDone(first);
        verify(objectStorageDeleteOutboxClaimService).markDone(second);
    }

    @Test
    @DisplayName("삭제에 성공하면 outbox를 DONE으로 표시한다")
    void processOne_marksDoneWhenDeleteSucceeds() {
        ReflectionTestUtils.setField(objectStorageDeleteOutboxProcessor, "maxRetryCount", 5);
        ReflectionTestUtils.setField(objectStorageDeleteOutboxProcessor, "processingTimeoutMinutes", 10L);
        ClaimedObjectStorageDeleteOutbox outbox = new ClaimedObjectStorageDeleteOutbox(1L, "chat/file.jpg", "token");
        given(objectStorageDeleteOutboxClaimService.claim(eq(1L), eq(5), any(LocalDateTime.class)))
                .willReturn(Optional.of(outbox));
        given(objectStorageDeleteOutboxClaimService.markDone(outbox)).willReturn(true);

        boolean processed = objectStorageDeleteOutboxProcessor.processOne(1L);

        assertThat(processed).isTrue();
        verify(objectStorageClient).delete("chat/file.jpg");
        verify(objectStorageDeleteOutboxClaimService).markDone(outbox);
    }

    @Test
    @DisplayName("삭제에 실패하면 실패 사유와 retryCount를 남긴다")
    void processOne_marksFailedWhenDeleteFails() {
        ReflectionTestUtils.setField(objectStorageDeleteOutboxProcessor, "maxRetryCount", 5);
        ReflectionTestUtils.setField(objectStorageDeleteOutboxProcessor, "processingTimeoutMinutes", 10L);
        ClaimedObjectStorageDeleteOutbox outbox = new ClaimedObjectStorageDeleteOutbox(1L, "chat/file.jpg", "token");
        given(objectStorageDeleteOutboxClaimService.claim(eq(1L), eq(5), any(LocalDateTime.class)))
                .willReturn(Optional.of(outbox));
        willThrow(new RuntimeException("delete failed"))
                .given(objectStorageClient)
                .delete("chat/file.jpg");
        given(objectStorageDeleteOutboxClaimService.markFailed(outbox, "delete failed")).willReturn(true);

        boolean processed = objectStorageDeleteOutboxProcessor.processOne(1L);

        assertThat(processed).isTrue();
        verify(objectStorageDeleteOutboxClaimService).markFailed(outbox, "delete failed");
    }

    @Test
    @DisplayName("다른 worker가 먼저 claim한 outbox는 처리하지 않는다")
    void processOne_skipsWhenClaimFails() {
        ReflectionTestUtils.setField(objectStorageDeleteOutboxProcessor, "maxRetryCount", 5);
        ReflectionTestUtils.setField(objectStorageDeleteOutboxProcessor, "processingTimeoutMinutes", 10L);
        given(objectStorageDeleteOutboxClaimService.claim(eq(1L), eq(5), any(LocalDateTime.class)))
                .willReturn(Optional.empty());

        boolean processed = objectStorageDeleteOutboxProcessor.processOne(1L);

        assertThat(processed).isFalse();
        verify(objectStorageClient, never()).delete(any());
    }
}
