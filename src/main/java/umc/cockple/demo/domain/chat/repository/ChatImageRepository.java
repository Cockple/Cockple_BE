package umc.cockple.demo.domain.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.cockple.demo.domain.chat.domain.ChatMessageFile;

public interface ChatImageRepository extends JpaRepository<ChatMessageFile, Long> {
}
