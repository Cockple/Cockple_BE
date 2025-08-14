package umc.cockple.demo.domain.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import umc.cockple.demo.domain.chat.domain.MessageReadStatus;

import java.util.List;

public interface MessageReadStatusRepository extends JpaRepository<MessageReadStatus, Long> {

    @Modifying
    @Query("""
            UPDATE MessageReadStatus mrs
            SET mrs.isRead = true
            WHERE mrs.chatMessageId = :messageId
            AND mrs.memberId IN :memberIds
            AND mrs.isRead = false
            """)
    int markAsReadInMembers(
            @Param("messageId") Long messageId,
            @Param("memberIds") List<Long> memberIds);
}
