package umc.cockple.demo.domain.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.cockple.demo.domain.chat.domain.MessageReadStatus;

public interface MessageReadStatusRepository extends JpaRepository<MessageReadStatus, Long> {
}
