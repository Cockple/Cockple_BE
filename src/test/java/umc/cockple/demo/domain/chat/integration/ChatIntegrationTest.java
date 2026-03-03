package umc.cockple.demo.domain.chat.integration;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import umc.cockple.demo.domain.chat.domain.ChatMessage;
import umc.cockple.demo.domain.chat.domain.ChatRoom;
import umc.cockple.demo.domain.chat.domain.ChatRoomMember;
import umc.cockple.demo.domain.chat.exception.ChatErrorCode;
import umc.cockple.demo.domain.chat.repository.ChatMessageRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomRepository;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.domain.PartyAddr;
import umc.cockple.demo.domain.party.repository.PartyAddrRepository;
import umc.cockple.demo.domain.party.repository.PartyRepository;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.global.enums.Role;
import umc.cockple.demo.support.IntegrationTestBase;
import umc.cockple.demo.support.SecurityContextHelper;
import umc.cockple.demo.support.fixture.ChatFixture;
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChatIntegrationTest extends IntegrationTestBase {

    @Autowired MockMvc mockMvc;
    @Autowired MemberRepository memberRepository;
    @Autowired PartyRepository partyRepository;
    @Autowired PartyAddrRepository partyAddrRepository;
    @Autowired MemberPartyRepository memberPartyRepository;
    @Autowired ChatRoomRepository chatRoomRepository;
    @Autowired ChatRoomMemberRepository chatRoomMemberRepository;
    @Autowired ChatMessageRepository chatMessageRepository;

    private Member member;
    private Member otherMember;
    private Party party;
    private ChatRoom partyChatRoom;
    private ChatRoom directChatRoom;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(MemberFixture.createMember("홍길동", Gender.MALE, Level.A, 1001L));
        otherMember = memberRepository.save(MemberFixture.createMember("김철수", Gender.MALE, Level.B, 2002L));

        PartyAddr addr = partyAddrRepository.save(PartyFixture.createPartyAddr("서울특별시", "강남구"));
        party = partyRepository.save(PartyFixture.createParty("배드민턴 모임", member.getId(), addr));

        memberPartyRepository.save(MemberFixture.createMemberParty(party, member, Role.party_MANAGER));
        memberPartyRepository.save(MemberFixture.createMemberParty(party, otherMember, Role.party_MEMBER));

        partyChatRoom = chatRoomRepository.save(ChatFixture.createPartyChatRoom(party));
        directChatRoom = chatRoomRepository.save(ChatFixture.createDirectChatRoom());
    }

    @AfterEach
    void tearDown() {
        chatMessageRepository.deleteAll();
        chatRoomMemberRepository.deleteAll();
        chatRoomRepository.deleteAll();
        memberPartyRepository.deleteAll();
        partyRepository.deleteAll();
        partyAddrRepository.deleteAll();
        memberRepository.deleteAll();
        SecurityContextHelper.clearAuthentication();
    }

    @Nested
    @DisplayName("GET /api/chats/rooms/{roomId} - 초기 채팅방 조회")
    class GetChatRoomDetail {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("200 - 모임(PARTY) 채팅방 조회 시 파티 이름이 displayName에 들어간다")
            void partyChatRoom_displayName_success() throws Exception {
                chatRoomMemberRepository.save(ChatRoomMember.create(partyChatRoom, member));
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/chats/rooms/{roomId}", partyChatRoom.getId()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.chatRoomInfo.displayName").value("배드민턴 모임"));
            }

            @Test
            @DisplayName("200 - 모임(PARTY) 채팅방 조회 시 모든 필드가 정확하게 반환된다")
            void partyChatRoom_fullFieldValidation() throws Exception {
                ChatRoomMember myCrm = ChatRoomMember.create(partyChatRoom, member);
                myCrm.updateLastReadMessageId(0L);
                chatRoomMemberRepository.save(myCrm);

                ChatMessage lastMsg = chatMessageRepository.save(
                        ChatFixture.createTextMessage(partyChatRoom, member, "최근 메시지"));

                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/chats/rooms/{roomId}", partyChatRoom.getId()))
                        .andExpect(status().isOk())
                        // 1. chatRoomInfo 전수 검사
                        .andExpect(jsonPath("$.data.chatRoomInfo.chatRoomId").value(partyChatRoom.getId()))
                        .andExpect(jsonPath("$.data.chatRoomInfo.chatRoomType").value("PARTY"))
                        .andExpect(jsonPath("$.data.chatRoomInfo.displayName").value("배드민턴 모임"))
                        .andExpect(jsonPath("$.data.chatRoomInfo.memberCount").value(1))
                        .andExpect(jsonPath("$.data.chatRoomInfo.lastReadMessageId").exists())
                        .andExpect(jsonPath("$.data.chatRoomInfo.isCounterPartWithdrawn").value(false))
                        // 2. messages 리스트 및 첫 번째 메시지 필드 전수 검사
                        .andExpect(jsonPath("$.data.messages").isArray())
                        .andExpect(jsonPath("$.data.messages[0].messageId").value(lastMsg.getId()))
                        .andExpect(jsonPath("$.data.messages[0].senderId").value(member.getId()))
                        .andExpect(jsonPath("$.data.messages[0].senderName").value("홍길동"))
                        .andExpect(jsonPath("$.data.messages[0].content").value("최근 메시지"))
                        .andExpect(jsonPath("$.data.messages[0].messageType").value("TEXT"))
                        .andExpect(jsonPath("$.data.messages[0].timestamp").exists())
                        .andExpect(jsonPath("$.data.messages[0].isMyMessage").value(true))
                        .andExpect(jsonPath("$.data.messages[0].isSenderWithdrawn").value(false))
                        // 3. participants 리스트 및 첫 번째 참여자 필드 전수 검사
                        .andExpect(jsonPath("$.data.participants").isArray())
                        .andExpect(jsonPath("$.data.participants[0].memberId").value(member.getId()))
                        .andExpect(jsonPath("$.data.participants[0].memberName").value("홍길동"));
            }

            @Test
            @DisplayName("200 - 개인(DIRECT) 채팅방 조회 시 상대방 이름이 displayName에 들어간다")
            void directChatRoom_success() throws Exception {
                chatRoomMemberRepository.save(ChatRoomMember.create(directChatRoom, member));
                chatRoomMemberRepository.save(ChatRoomMember.create(directChatRoom, otherMember));

                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/chats/rooms/{roomId}", directChatRoom.getId()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.chatRoomInfo.chatRoomType").value("DIRECT"))
                        .andExpect(jsonPath("$.data.chatRoomInfo.displayName").value("김철수"))
                        .andExpect(jsonPath("$.data.chatRoomInfo.isCounterPartWithdrawn").value(false));
            }

            @Test
            @DisplayName("200 - 개인 채팅방에서 상대방이 탈퇴한 경우 isCounterPartWithdrawn이 true이다")
            void directChatRoom_counterPartWithdrawn() throws Exception {
                Member withdrawnMember = memberRepository.save(
                        MemberFixture.createWithdrawnMember("탈퇴한사용자", "탈퇴", 3003L));

                chatRoomMemberRepository.save(ChatRoomMember.create(directChatRoom, member));
                chatRoomMemberRepository.save(ChatRoomMember.create(directChatRoom, withdrawnMember));

                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/chats/rooms/{roomId}", directChatRoom.getId()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.chatRoomInfo.isCounterPartWithdrawn").value(true));
            }

            @Test
            @DisplayName("200 - 최근 메시지가 오래된 순으로 정렬되어 반환된다")
            void messages_areInChronologicalOrder() throws Exception {
                chatRoomMemberRepository.save(ChatRoomMember.create(partyChatRoom, member));

                ChatMessage msg1 = chatMessageRepository.save(
                        ChatFixture.createTextMessage(partyChatRoom, member, "첫 번째 메시지"));
                ChatMessage msg2 = chatMessageRepository.save(
                        ChatFixture.createTextMessage(partyChatRoom, member, "두 번째 메시지"));
                ChatMessage msg3 = chatMessageRepository.save(
                        ChatFixture.createTextMessage(partyChatRoom, member, "세 번째 메시지"));

                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/chats/rooms/{roomId}", partyChatRoom.getId()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.messages", hasSize(3)))
                        .andExpect(jsonPath("$.data.messages[0].messageId").value(msg1.getId()))
                        .andExpect(jsonPath("$.data.messages[1].messageId").value(msg2.getId()))
                        .andExpect(jsonPath("$.data.messages[2].messageId").value(msg3.getId()));
            }

            @Test
            @DisplayName("200 - 내가 보낸 메시지는 isMyMessage가 true이다")
            void myMessage_isMyMessageTrue() throws Exception {
                chatRoomMemberRepository.save(ChatRoomMember.create(partyChatRoom, member));
                chatMessageRepository.save(
                        ChatFixture.createTextMessage(partyChatRoom, member, "내 메시지"));

                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/chats/rooms/{roomId}", partyChatRoom.getId()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.messages[0].isMyMessage").value(true))
                        .andExpect(jsonPath("$.data.messages[0].content").value("내 메시지"));
            }

            @Test
            @DisplayName("200 - 상대방이 보낸 메시지는 isMyMessage가 false이다")
            void otherMessage_isMyMessageFalse() throws Exception {
                chatRoomMemberRepository.save(ChatRoomMember.create(partyChatRoom, member));
                chatRoomMemberRepository.save(ChatRoomMember.create(partyChatRoom, otherMember));
                chatMessageRepository.save(
                        ChatFixture.createTextMessage(partyChatRoom, otherMember, "상대방 메시지"));

                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/chats/rooms/{roomId}", partyChatRoom.getId()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.messages[0].isMyMessage").value(false))
                        .andExpect(jsonPath("$.data.messages[0].isSenderWithdrawn").value(false));
            }

            @Test
            @DisplayName("200 - 탈퇴한 사용자의 메시지는 isSenderWithdrawn이 true이다")
            void withdrawnSender_isSenderWithdrawnTrue() throws Exception {
                Member withdrawnMember = memberRepository.save(
                        MemberFixture.createWithdrawnMember("탈퇴한사용자", "탈퇴", 3003L));

                chatRoomMemberRepository.save(ChatRoomMember.create(partyChatRoom, member));
                chatRoomMemberRepository.save(ChatRoomMember.create(partyChatRoom, withdrawnMember));
                chatMessageRepository.save(
                        ChatFixture.createTextMessage(partyChatRoom, withdrawnMember, "탈퇴자 메시지"));

                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/chats/rooms/{roomId}", partyChatRoom.getId()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.messages[0].isSenderWithdrawn").value(true));
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("404 - 존재하지 않는 채팅방 조회 시 CHAT_ROOM_NOT_FOUND 에러를 반환한다")
            void chatRoomNotFound() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/chats/rooms/{roomId}", 999L))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value(ChatErrorCode.CHAT_ROOM_NOT_FOUND.getCode()))
                        .andExpect(jsonPath("$.message").value(ChatErrorCode.CHAT_ROOM_NOT_FOUND.getMessage()));
            }

            @Test
            @DisplayName("400 - 채팅방 멤버가 아닌 사용자가 조회하면 CHAT_ROOM_ACCESS_DENIED 에러를 반환한다")
            void notChatRoomMember() throws Exception {
                // partyChatRoom에 member만 가입, otherMember는 비가입
                chatRoomMemberRepository.save(ChatRoomMember.create(partyChatRoom, member));

                SecurityContextHelper.setAuthentication(otherMember.getId(), otherMember.getNickname());

                mockMvc.perform(get("/api/chats/rooms/{roomId}", partyChatRoom.getId()))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED.getCode()))
                        .andExpect(jsonPath("$.message").value(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED.getMessage()));
            }
        }
    }
}
