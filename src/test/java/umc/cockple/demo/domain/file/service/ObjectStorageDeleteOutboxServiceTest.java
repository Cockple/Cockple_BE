package umc.cockple.demo.domain.file.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.cockple.demo.domain.file.domain.ObjectStorageDeleteOutbox;
import umc.cockple.demo.domain.file.enums.ObjectStorageDeleteSourceType;
import umc.cockple.demo.domain.file.enums.ObjectStorageDeleteStatus;
import umc.cockple.demo.domain.file.repository.ObjectStorageDeleteOutboxRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ObjectStorageDeleteOutboxService 단위 테스트")
class ObjectStorageDeleteOutboxServiceTest {

    @InjectMocks
    private ObjectStorageDeleteOutboxService objectStorageDeleteOutboxService;

    @Mock
    private ObjectStorageDeleteOutboxRepository objectStorageDeleteOutboxRepository;

    @Test
    @DisplayName("채팅 object key를 중복 제거 후 PARTY_CHAT_ROOM outbox로 등록한다")
    void enqueuePartyChatFiles_savesDistinctObjectKeys() {
        Long chatRoomId = 10L;
        List<String> objectKeys = List.of("chat/a.jpg", "chat/a.jpg", "", "chat/b.jpg");

        objectStorageDeleteOutboxService.enqueuePartyChatFiles(chatRoomId, objectKeys);

        ArgumentCaptor<List<ObjectStorageDeleteOutbox>> captor = ArgumentCaptor.forClass(List.class);
        verify(objectStorageDeleteOutboxRepository).saveAll(captor.capture());

        List<ObjectStorageDeleteOutbox> savedOutboxes = captor.getValue();
        assertThat(savedOutboxes)
                .extracting(ObjectStorageDeleteOutbox::getObjectKey)
                .containsExactly("chat/a.jpg", "chat/b.jpg");
        assertThat(savedOutboxes)
                .allSatisfy(outbox -> {
                    assertThat(outbox.getSourceType()).isEqualTo(ObjectStorageDeleteSourceType.PARTY_CHAT_ROOM);
                    assertThat(outbox.getSourceId()).isEqualTo(chatRoomId);
                    assertThat(outbox.getStatus()).isEqualTo(ObjectStorageDeleteStatus.PENDING);
                    assertThat(outbox.getRetryCount()).isZero();
                });
    }

    @Test
    @DisplayName("등록할 object key가 없으면 저장하지 않는다")
    void enqueuePartyChatFiles_skipsWhenNoObjectKeys() {
        objectStorageDeleteOutboxService.enqueuePartyChatFiles(10L, List.of("", " "));

        verify(objectStorageDeleteOutboxRepository, never()).saveAll(org.mockito.ArgumentMatchers.any());
    }
}
