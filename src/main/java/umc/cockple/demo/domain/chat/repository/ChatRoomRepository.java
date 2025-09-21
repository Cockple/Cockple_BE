package umc.cockple.demo.domain.chat.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import umc.cockple.demo.domain.chat.domain.ChatRoom;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    @Query("""
            SELECT cr FROM ChatRoom cr
            JOIN cr.chatRoomMembers crm
            WHERE crm.member.id = :memberId
              AND cr.type = 'PARTY'
            ORDER BY (
                SELECT MAX(cm.id)
                FROM ChatMessage cm
                WHERE cm.chatRoom.id = cr.id
            ) DESC
            """)
    Slice<ChatRoom> findPartyChatRoomByMemberIdOrderByLastMsgIdDesc(
            @Param("memberId") Long memberId,
            Pageable pageable
    );

    @Query("""
            SELECT cr FROM ChatRoom cr
            JOIN cr.chatRoomMembers crm
            WHERE crm.member.id = :memberId
              AND cr.party.partyName LIKE %:name%
            ORDER BY (
                SELECT MAX(cm.id)
                FROM ChatMessage cm
                WHERE cm.chatRoom.id = cr.id
            ) DESC
            """)
    Slice<ChatRoom> searchPartyChatRoomsByName(
            @Param("memberId") Long memberId,
            @Param("name") String name,
            Pageable pageable
    );

    Optional<ChatRoom> findByPartyId(Long partyId);

    @Query("""
            SELECT cr
            FROM ChatRoom cr
            JOIN cr.chatRoomMembers m
            WHERE cr.type = 'DIRECT'
              AND m.member.id IN (:memberId1, :memberId2)
            GROUP BY cr.id
            HAVING COUNT(DISTINCT m.member.id) = 2
            """)
    Optional<ChatRoom> findDirectChatRoomByMemberIds(
            @Param("memberId1") Long memberId1,
            @Param("memberId2") Long memberId2
    );

    @Query("""
            SELECT cr FROM ChatRoom cr
            JOIN cr.chatRoomMembers crm
            WHERE crm.member.id = :memberId
            AND cr.type = 'DIRECT'
            AND crm.status = 'JOINED' 
            ORDER BY (
                SELECT MAX(cm.id)
                FROM ChatMessage cm
                WHERE cm.chatRoom.id = cr.id
            ) DESC
            """)
    Slice<ChatRoom> findDirectChatRoomByMemberIdOrderByLastMsgIdDesc(
            @Param("memberId") Long memberId,
            Pageable pageable
    );

    @Query("""
            SELECT cr FROM ChatRoom cr
            JOIN cr.chatRoomMembers crm
            WHERE crm.member.id = :memberId
            AND cr.type = 'DIRECT'
            AND crm.status = 'JOINED' 
            AND crm.displayName LIKE %:name%
            ORDER BY (
                SELECT MAX(cm.id)
                FROM ChatMessage cm
                WHERE cm.chatRoom.id = cr.id
            ) DESC
            """)
    Slice<ChatRoom> searchDirectChatRoomsByName(
            @Param("memberId") Long memberId,
            @Param("name") String name,
            Pageable pageable
    );

    @Query("""
            SELECT cr FROM ChatRoom cr
            LEFT JOIN FETCH cr.party p
            LEFT JOIN FETCH p.partyImg img
            WHERE cr.id = :roomId
            """)
    Optional<ChatRoom> findChatRoomWithPartyById(@Param("roomId") Long roomId);
}