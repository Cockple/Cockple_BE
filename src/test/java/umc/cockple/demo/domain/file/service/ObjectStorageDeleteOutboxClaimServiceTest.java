package umc.cockple.demo.domain.file.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import umc.cockple.demo.domain.file.domain.ObjectStorageDeleteOutbox;
import umc.cockple.demo.domain.file.enums.ObjectStorageDeleteSourceType;
import umc.cockple.demo.domain.file.enums.ObjectStorageDeleteStatus;
import umc.cockple.demo.domain.file.repository.ObjectStorageDeleteOutboxRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("ObjectStorageDeleteOutboxClaimService 단위 테스트")
class ObjectStorageDeleteOutboxClaimServiceTest {

    @InjectMocks
    private ObjectStorageDeleteOutboxClaimService objectStorageDeleteOutboxClaimService;

    @Mock
    private ObjectStorageDeleteOutboxRepository objectStorageDeleteOutboxRepository;

    @Test
    @DisplayName("claim 성공 시 PROCESSING으로 선점한 outbox 정보를 반환한다")
    void claim_returnsClaimedOutboxWhenUpdateSucceeds() {
        Long outboxId = 1L;
        ObjectStorageDeleteOutbox outbox = ObjectStorageDeleteOutbox.pending(
                "chat/file.jpg",
                ObjectStorageDeleteSourceType.PARTY_CHAT_ROOM,
                10L
        );
        ReflectionTestUtils.setField(outbox, "id", outboxId);
        given(objectStorageDeleteOutboxRepository.claimForProcessing(
                eq(outboxId),
                eq(List.of(ObjectStorageDeleteStatus.PENDING, ObjectStorageDeleteStatus.FAILED)),
                eq(ObjectStorageDeleteStatus.PROCESSING),
                eq(5),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                any(String.class)
        )).willReturn(1);
        given(objectStorageDeleteOutboxRepository.findByIdAndStatusAndClaimToken(
                eq(outboxId),
                eq(ObjectStorageDeleteStatus.PROCESSING),
                any(String.class)
        )).willReturn(Optional.of(outbox));

        Optional<ClaimedObjectStorageDeleteOutbox> claimedOutbox = objectStorageDeleteOutboxClaimService.claim(
                outboxId,
                5,
                LocalDateTime.now().minusMinutes(10)
        );

        assertThat(claimedOutbox).isPresent();
        assertThat(claimedOutbox.get().id()).isEqualTo(outboxId);
        assertThat(claimedOutbox.get().objectKey()).isEqualTo("chat/file.jpg");
        assertThat(claimedOutbox.get().claimToken()).isNotBlank();
    }

    @Test
    @DisplayName("이미 다른 worker가 선점한 outbox는 빈 결과를 반환한다")
    void claim_returnsEmptyWhenUpdateSkipped() {
        Long outboxId = 1L;
        given(objectStorageDeleteOutboxRepository.claimForProcessing(
                eq(outboxId),
                eq(List.of(ObjectStorageDeleteStatus.PENDING, ObjectStorageDeleteStatus.FAILED)),
                eq(ObjectStorageDeleteStatus.PROCESSING),
                eq(5),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                any(String.class)
        )).willReturn(0);
        given(objectStorageDeleteOutboxRepository.existsById(outboxId)).willReturn(true);

        Optional<ClaimedObjectStorageDeleteOutbox> claimedOutbox = objectStorageDeleteOutboxClaimService.claim(
                outboxId,
                5,
                LocalDateTime.now().minusMinutes(10)
        );

        assertThat(claimedOutbox).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 outbox claim은 예외를 반환한다")
    void claim_throwsWhenOutboxMissing() {
        Long outboxId = 1L;
        given(objectStorageDeleteOutboxRepository.claimForProcessing(
                eq(outboxId),
                eq(List.of(ObjectStorageDeleteStatus.PENDING, ObjectStorageDeleteStatus.FAILED)),
                eq(ObjectStorageDeleteStatus.PROCESSING),
                eq(5),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                any(String.class)
        )).willReturn(0);
        given(objectStorageDeleteOutboxRepository.existsById(outboxId)).willReturn(false);

        assertThatThrownBy(() -> objectStorageDeleteOutboxClaimService.claim(
                outboxId,
                5,
                LocalDateTime.now().minusMinutes(10)
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("claim token이 일치하는 PROCESSING outbox만 DONE으로 변경한다")
    void markDone_marksOnlyClaimedOutbox() {
        ObjectStorageDeleteOutbox outbox = ObjectStorageDeleteOutbox.pending(
                "chat/file.jpg",
                ObjectStorageDeleteSourceType.PARTY_CHAT_ROOM,
                10L
        );
        ClaimedObjectStorageDeleteOutbox claimedOutbox = new ClaimedObjectStorageDeleteOutbox(1L, "chat/file.jpg", "token");
        given(objectStorageDeleteOutboxRepository.findByIdAndStatusAndClaimToken(
                1L,
                ObjectStorageDeleteStatus.PROCESSING,
                "token"
        )).willReturn(Optional.of(outbox));

        boolean marked = objectStorageDeleteOutboxClaimService.markDone(claimedOutbox);

        assertThat(marked).isTrue();
        assertThat(outbox.getStatus()).isEqualTo(ObjectStorageDeleteStatus.DONE);
        assertThat(outbox.getClaimToken()).isNull();
    }

    @Test
    @DisplayName("claim token이 일치하는 PROCESSING outbox만 FAILED로 변경한다")
    void markFailed_marksOnlyClaimedOutbox() {
        ObjectStorageDeleteOutbox outbox = ObjectStorageDeleteOutbox.pending(
                "chat/file.jpg",
                ObjectStorageDeleteSourceType.PARTY_CHAT_ROOM,
                10L
        );
        ClaimedObjectStorageDeleteOutbox claimedOutbox = new ClaimedObjectStorageDeleteOutbox(1L, "chat/file.jpg", "token");
        given(objectStorageDeleteOutboxRepository.findByIdAndStatusAndClaimToken(
                1L,
                ObjectStorageDeleteStatus.PROCESSING,
                "token"
        )).willReturn(Optional.of(outbox));

        boolean marked = objectStorageDeleteOutboxClaimService.markFailed(claimedOutbox, "delete failed");

        assertThat(marked).isTrue();
        assertThat(outbox.getStatus()).isEqualTo(ObjectStorageDeleteStatus.FAILED);
        assertThat(outbox.getRetryCount()).isEqualTo(1);
        assertThat(outbox.getLastError()).isEqualTo("delete failed");
        assertThat(outbox.getClaimToken()).isNull();
    }
}
