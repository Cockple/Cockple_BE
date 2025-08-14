package umc.cockple.demo.domain.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import umc.cockple.demo.domain.chat.domain.ChatRoomMember;

import java.util.List;
import java.util.Optional;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {

    // 채팅방 내 참여자 수
    @Query("SELECT COUNT(c) FROM ChatRoomMember c WHERE c.chatRoom.id = :chatRoomId")
    int countByChatRoomId(@Param("chatRoomId") Long chatRoomId);

    // 특정 채팅방에 참여한 특정 멤버 조회
    @Query("""
                SELECT c FROM ChatRoomMember c
                WHERE c.chatRoom.id = :chatRoomId AND c.member.id = :memberId
            """)
    Optional<ChatRoomMember> findByChatRoomIdAndMemberId(
            @Param("chatRoomId") Long chatRoomId,
            @Param("memberId") Long memberId
    );

    List<ChatRoomMember> findByChatRoomId(Long id);

    List<ChatRoomMember> findAllByMemberId(Long id);

    @Query("""
            SELECT crm FROM ChatRoomMember crm
            JOIN FETCH crm.member m
            LEFT JOIN FETCH m.profileImg
            WHERE crm.chatRoom.id = :chatRoomId
            AND crm.member.id != :myId
            """)
    Optional<ChatRoomMember> findCounterPartWithMember(
            @Param("chatRoomId") Long chatRoomId,
            @Param("myId") Long myId
    );

    @Query("""
            SELECT crm FROM ChatRoomMember crm
            JOIN FETCH crm.member m
            LEFT JOIN FETCH m.profileImg
            WHERE crm.chatRoom.id = :chatRoomId
            ORDER BY m.memberName ASC
            """)
    List<ChatRoomMember> findChatRoomMembersWithMemberById(@Param("chatRoomId") Long chatRoomId);

    Boolean existsByChatRoomIdAndMemberId(Long roomId, Long memberId);

    @Query("""
            SELECT crm FROM ChatRoomMember crm
            JOIN FETCH crm.member m
            WHERE crm.chatRoom.id = :chatRoomId
            AND crm.member.id IN :memberIds
            """)
    List<ChatRoomMember> findChatRoomMembersInChatRoom(
            @Param("chatRoomId") Long chatRoomId,
            @Param("memberIds") List<Long> memberIds);

    @Query("""
            SELECT crm FROM ChatRoomMember crm
            JOIN FETCH crm.member m
            WHERE crm.chatRoom.id = :chatRoomId 
            AND crm.member.id != :senderId
            AND crm.status = 'PENDING'
            """)
    Optional<ChatRoomMember> findPendingMemberInDirect(Long chatRoomId, Long senderId);

    @Query("SELECT crm.member.id FROM ChatRoomMember crm WHERE crm.chatRoom.id = :chatRoomId")
    List<Long> findMemberIdsByChatRoomId(Long chatRoomId);
}

