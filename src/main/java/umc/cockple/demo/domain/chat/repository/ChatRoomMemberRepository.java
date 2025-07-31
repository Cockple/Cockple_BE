package umc.cockple.demo.domain.chat.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import umc.cockple.demo.domain.chat.domain.ChatRoom;
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

}

