package umc.cockple.demo.domain.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import umc.cockple.demo.domain.chat.domain.MessageReadStatus;

import java.util.List;

public interface MessageReadStatusRepository extends JpaRepository<MessageReadStatus, Long> {

    @Modifying(flushAutomatically = true)
    @Query("""
            DELETE FROM MessageReadStatus mrs
            WHERE mrs.chatRoomId = :chatRoomId
            """)
    int deleteByChatRoomId(@Param("chatRoomId") Long chatRoomId);

    @Modifying(flushAutomatically = true)
    @Query("""
            DELETE FROM MessageReadStatus mrs
            WHERE mrs.memberId = :memberId
            """)
    int deleteByMemberId(@Param("memberId") Long memberId);

    @Modifying(flushAutomatically = true)
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
            SELECT CASE WHEN COUNT(mrs) > 0 THEN true ELSE false END FROM MessageReadStatus mrs
            WHERE mrs.memberId = :memberId
            AND mrs.isRead = false
            AND EXISTS (
                SELECT 1 FROM ChatRoomMember crm
                WHERE crm.chatRoom.id = mrs.chatRoomId
                AND crm.member.id = :memberId
                AND crm.chatRoom.type = 'PARTY'
                AND (crm.lastReadMessageId IS NULL OR mrs.chatMessageId > crm.lastReadMessageId)
            )
            """)
    boolean existsPartyUnreadMessagesByMemberId(@Param("memberId") Long memberId);

    @Query("""
            SELECT CASE WHEN COUNT(mrs) > 0 THEN true ELSE false END FROM MessageReadStatus mrs
            WHERE mrs.memberId = :memberId
            AND mrs.isRead = false
            AND EXISTS (
                SELECT 1 FROM ChatRoomMember crm
                WHERE crm.chatRoom.id = mrs.chatRoomId
                AND crm.member.id = :memberId
                AND crm.chatRoom.type = 'DIRECT'
                AND crm.status = 'JOINED'
                AND (crm.lastReadMessageId IS NULL OR mrs.chatMessageId > crm.lastReadMessageId)
            )
            """)
    boolean existsDirectUnreadMessagesByMemberId(@Param("memberId") Long memberId);

    @Query("""
            SELECT COUNT(mrs) FROM MessageReadStatus mrs
            WHERE mrs.chatMessageId = :messageId
            AND mrs.isRead = false
            """)
    int countUnreadByMessageId(
            @Param("messageId") Long messageId);

    @Query("""
            SELECT mrs.chatMessageId FROM MessageReadStatus mrs
            WHERE mrs.chatRoomId = :chatRoomId
            AND mrs.memberId = :memberId
            AND mrs.isRead = false
            ORDER BY mrs.chatMessageId ASC
            """)
    List<Long> findUnreadMessageIdsByMember(Long chatRoomId, Long memberId);

}
