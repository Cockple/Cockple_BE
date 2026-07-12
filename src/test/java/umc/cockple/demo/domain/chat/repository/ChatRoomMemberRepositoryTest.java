package umc.cockple.demo.domain.chat.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import umc.cockple.demo.domain.chat.domain.ChatRoom;
import umc.cockple.demo.domain.chat.domain.ChatRoomMember;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.global.config.QuerydslConfig;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.fixture.ChatFixture;
import umc.cockple.demo.support.fixture.MemberFixture;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ChatRoomMemberRepository")
@DataJpaTest
@Import(QuerydslConfig.class)
class ChatRoomMemberRepositoryTest {

    @Autowired private ChatRoomMemberRepository chatRoomMemberRepository;
    @Autowired private ChatRoomRepository chatRoomRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private TestEntityManager entityManager;

    @Test
    @DisplayName("advanceLastReadMessageId는 기존 값이 없으면 새 메시지 ID로 설정한다")
    void advanceLastReadMessageId_updatesNullValue() {
        // given
        Member member = memberRepository.save(MemberFixture.createMember("홍길동", Gender.MALE, Level.A, 1001L));
        ChatRoom chatRoom = chatRoomRepository.save(ChatFixture.createDirectChatRoom());
        ChatRoomMember membership =
                chatRoomMemberRepository.save(ChatRoomMember.createJoined(chatRoom, member, "상대방"));

        // when
        int updatedCount = chatRoomMemberRepository.advanceLastReadMessageId(chatRoom.getId(), member.getId(), 100L);

        // then
        assertThat(updatedCount).isEqualTo(1);
        entityManager.clear();
        ChatRoomMember updatedMembership = chatRoomMemberRepository.findById(membership.getId()).orElseThrow();
        assertThat(updatedMembership.getLastReadMessageId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("advanceLastReadMessageId는 기존 값보다 작은 메시지 ID로 감소시키지 않는다")
    void advanceLastReadMessageId_doesNotMoveBackward() {
        // given
        Member member = memberRepository.save(MemberFixture.createMember("홍길동", Gender.MALE, Level.A, 1001L));
        ChatRoom chatRoom = chatRoomRepository.save(ChatFixture.createDirectChatRoom());
        ChatRoomMember membership = chatRoomMemberRepository.save(
                ChatFixture.createJoinedMemberWithLastRead(chatRoom, member, 200L));

        // when
        int updatedCount = chatRoomMemberRepository.advanceLastReadMessageId(chatRoom.getId(), member.getId(), 100L);

        // then
        assertThat(updatedCount).isEqualTo(0);
        entityManager.clear();
        ChatRoomMember updatedMembership = chatRoomMemberRepository.findById(membership.getId()).orElseThrow();
        assertThat(updatedMembership.getLastReadMessageId()).isEqualTo(200L);
    }

    @Test
    @DisplayName("advanceLastReadMessageIdForMembers는 여러 멤버의 lastReadMessageId를 전진시킨다")
    void advanceLastReadMessageIdForMembers_updatesMultipleMembers() {
        // given
        Member firstMember = memberRepository.save(MemberFixture.createMember("첫번째", Gender.MALE, Level.A, 1001L));
        Member secondMember = memberRepository.save(MemberFixture.createMember("두번째", Gender.FEMALE, Level.B, 2002L));
        Member skippedMember = memberRepository.save(MemberFixture.createMember("스킵", Gender.MALE, Level.C, 3003L));
        ChatRoom chatRoom = chatRoomRepository.save(ChatFixture.createDirectChatRoom());
        ChatRoomMember firstMembership =
                chatRoomMemberRepository.save(ChatRoomMember.createJoined(chatRoom, firstMember, "첫번째"));
        ChatRoomMember secondMembership = chatRoomMemberRepository.save(
                ChatFixture.createJoinedMemberWithLastRead(chatRoom, secondMember, 50L));
        ChatRoomMember skippedMembership = chatRoomMemberRepository.save(
                ChatFixture.createJoinedMemberWithLastRead(chatRoom, skippedMember, 300L));

        // when
        int updatedCount = chatRoomMemberRepository.advanceLastReadMessageIdForMembers(
                chatRoom.getId(),
                List.of(firstMember.getId(), secondMember.getId(), skippedMember.getId()),
                200L);

        // then
        assertThat(updatedCount).isEqualTo(2);
        entityManager.clear();
        assertThat(chatRoomMemberRepository.findById(firstMembership.getId()).orElseThrow().getLastReadMessageId())
                .isEqualTo(200L);
        assertThat(chatRoomMemberRepository.findById(secondMembership.getId()).orElseThrow().getLastReadMessageId())
                .isEqualTo(200L);
        assertThat(chatRoomMemberRepository.findById(skippedMembership.getId()).orElseThrow().getLastReadMessageId())
                .isEqualTo(300L);
    }
}
