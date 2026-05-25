package umc.cockple.demo.domain.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import umc.cockple.demo.domain.chat.domain.ChatMessageFile;

public interface ChatFileRepository extends JpaRepository<ChatMessageFile, Long> {

    @Modifying
    @Query("""
            DELETE FROM ChatMessageFile cmf
            WHERE cmf.chatMessage.chatRoom.id = :chatRoomId
            """)
    int deleteByChatRoomId(@Param("chatRoomId") Long chatRoomId);
}
