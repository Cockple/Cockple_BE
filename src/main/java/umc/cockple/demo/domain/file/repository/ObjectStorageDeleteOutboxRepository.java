package umc.cockple.demo.domain.file.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import umc.cockple.demo.domain.file.domain.ObjectStorageDeleteOutbox;
import umc.cockple.demo.domain.file.enums.ObjectStorageDeleteStatus;

import java.util.Collection;
import java.util.List;

public interface ObjectStorageDeleteOutboxRepository extends JpaRepository<ObjectStorageDeleteOutbox, Long> {

    List<ObjectStorageDeleteOutbox> findByStatusInAndRetryCountLessThanOrderByCreatedAtAsc(
            Collection<ObjectStorageDeleteStatus> statuses,
            int retryCount,
            Pageable pageable
    );
}
