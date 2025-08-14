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

    @Query("""
            SELECT COUNT(mrs) FROM MessageReadStatus mrs
            WHERE mrs.chatRoomId = :chatRoomId
            AND mrs.memberId = :memberId
            AND mrs.chatMessageId > :lastReadMessageId
            AND mrs.isRead = false
            """)
    int countUnreadMessagesAfter(
            @Param("chatRoomId") Long chatRoomId,
            @Param("memberId") Long memberId,
            @Param("lastReadMessageId") Long lastReadMessageId
    );


    @Query("""
            SELECT COUNT(mrs) FROM MessageReadStatus mrs
            WHERE mrs.chatRoomId = :chatRoomId
            AND mrs.memberId = :memberId
            AND mrs.isRead = false
            """)
    int countAllUnreadMessages(
            @Param("chatRoomId") Long chatRoomId,
            @Param("memberId") Long memberId
    );

    @Query("""
            SELECT COUNT(mrs) FROM MessageReadStatus mrs
            WHERE mrs.chatMessageId = :messageId
            AND mrs.isRead = false
            """)
    int countUnreadByMessageId(
            @Param("messageId") Long messageId);
}
