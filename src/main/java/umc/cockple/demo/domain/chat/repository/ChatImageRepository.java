package umc.cockple.demo.domain.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.cockple.demo.domain.chat.domain.ChatMessageImg;

public interface ChatImageRepository extends JpaRepository<ChatMessageImg, Long> {
}
