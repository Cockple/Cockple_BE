package umc.cockple.demo.domain.file.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ObjectStorageDeleteOutboxScheduler 단위 테스트")
class ObjectStorageDeleteOutboxSchedulerTest {

    @InjectMocks
    private ObjectStorageDeleteOutboxScheduler objectStorageDeleteOutboxScheduler;

    @Mock
    private ObjectStorageDeleteOutboxProcessor objectStorageDeleteOutboxProcessor;

    @Test
    @DisplayName("스케줄 실행 시 pending outbox 처리를 위임한다")
    void processPendingDeletes_delegatesToProcessor() {
        objectStorageDeleteOutboxScheduler.processPendingDeletes();

        verify(objectStorageDeleteOutboxProcessor).processPendingBatch();
    }
}
