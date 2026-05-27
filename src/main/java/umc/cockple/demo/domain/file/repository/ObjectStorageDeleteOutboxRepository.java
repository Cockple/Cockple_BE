package umc.cockple.demo.domain.file.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.cockple.demo.domain.file.domain.ObjectStorageDeleteOutbox;

public interface ObjectStorageDeleteOutboxRepository extends JpaRepository<ObjectStorageDeleteOutbox, Long> {
}
