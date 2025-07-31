package umc.cockple.demo.domain.chat.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import umc.cockple.demo.domain.chat.domain.ChatRoom;

import java.util.List;

public interface ChatRoomRepository extends JpaRepository<ChatRoom,Long> {

    @Query("""
    SELECT cr FROM ChatRoom cr
    JOIN cr.chatRoomMembers crm
    WHERE crm.member.id = :memberId
    AND (:cursor IS NULL OR
         (:direction = 'desc' AND cr.id < :cursor) OR
         (:direction = 'asc' AND cr.id > :cursor))
    ORDER BY
        CASE WHEN :direction = 'asc' THEN cr.id END ASC,
        CASE WHEN :direction = 'desc' THEN cr.id END DESC
""")
    List<ChatRoom> findPartyChatRoomsByMemberId(
            @Param("memberId") Long memberId,
            @Param("cursor") Long cursor,
            @Param("direction") String direction,
            Pageable pageable
    );

    @Query("""
    SELECT cr FROM ChatRoom cr
    JOIN cr.chatRoomMembers crm
    WHERE crm.member.id = :memberId
    AND cr.party.partyName LIKE %:name%
    AND (:cursor IS NULL OR
         (:direction = 'desc' AND cr.id < :cursor) OR
         (:direction = 'asc' AND cr.id > :cursor))
    ORDER BY
        CASE WHEN :direction = 'asc' THEN cr.id END ASC,
        CASE WHEN :direction = 'desc' THEN cr.id END DESC
    """)
    List<ChatRoom> searchPartyChatRoomsByName(
            @Param("memberId") Long memberId,
            @Param("name") String name,
            @Param("cursor") Long cursor,
            @Param("direction") String direction,
            Pageable pageable
    );
}
