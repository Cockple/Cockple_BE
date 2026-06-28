package umc.cockple.demo.domain.file.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import umc.cockple.demo.domain.file.domain.ObjectStorageDeleteOutbox;
import umc.cockple.demo.domain.file.enums.ObjectStorageDeleteSourceType;
import umc.cockple.demo.domain.file.repository.ObjectStorageDeleteOutboxRepository;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ObjectStorageDeleteOutboxService {

    private final ObjectStorageDeleteOutboxRepository objectStorageDeleteOutboxRepository;

    @Transactional
    public void enqueuePartyChatFiles(Long chatRoomId, Collection<String> objectKeys) {
        if (objectKeys == null || objectKeys.isEmpty()) {
            return;
        }

        List<ObjectStorageDeleteOutbox> outboxes = objectKeys.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .map(objectKey -> ObjectStorageDeleteOutbox.pending(
                        objectKey,
                        ObjectStorageDeleteSourceType.PARTY_CHAT_ROOM,
                        chatRoomId
                ))
                .toList();

        if (outboxes.isEmpty()) {
            return;
        }

        objectStorageDeleteOutboxRepository.saveAll(outboxes);
        log.info("Object storage 삭제 outbox 등록 - chatRoomId: {}, 파일 수: {}", chatRoomId, outboxes.size());
    }

    @Transactional
    public void enqueueProfileImage(Long memberId, String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            return;
        }

        objectStorageDeleteOutboxRepository.save(
                ObjectStorageDeleteOutbox.pending(
                        objectKey,
                        ObjectStorageDeleteSourceType.MEMBER_PROFILE_IMG,
                        memberId
                )
        );
        log.info("Object storage 삭제 outbox 등록 - memberId: {}, objectKey: {}", memberId, objectKey);
    }
}
