package umc.cockple.demo.domain.chat.integration;

import org.junit.jupiter.api.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import umc.cockple.demo.domain.chat.domain.ChatMessage;
import umc.cockple.demo.domain.chat.domain.ChatMessageFile;
import umc.cockple.demo.domain.chat.domain.ChatRoom;
import umc.cockple.demo.domain.chat.domain.ChatRoomMember;
import umc.cockple.demo.domain.chat.domain.MessageReadStatus;
import umc.cockple.demo.domain.chat.enums.MessageType;
import umc.cockple.demo.domain.chat.exception.ChatErrorCode;
import umc.cockple.demo.domain.chat.repository.ChatMessageRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomRepository;
import umc.cockple.demo.domain.chat.repository.MessageReadStatusRepository;
import umc.cockple.demo.domain.chat.service.ChatMemberHardDeleteCleanupService;
import umc.cockple.demo.domain.chat.service.websocket.ChatRoomListCacheService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.ProfileImg;
import umc.cockple.demo.domain.member.events.MemberWithdrawnEvent;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.domain.PartyAddr;
import umc.cockple.demo.domain.party.domain.PartyImg;
import umc.cockple.demo.domain.party.repository.PartyAddrRepository;
import umc.cockple.demo.domain.party.repository.PartyRepository;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.global.enums.Role;
import umc.cockple.demo.global.response.code.status.CommonSuccessCode;
import umc.cockple.demo.support.IntegrationTestBase;
import umc.cockple.demo.support.SecurityContextHelper;
import umc.cockple.demo.support.fixture.ChatFixture;
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    @Autowired MessageReadStatusRepository messageReadStatusRepository;
    @Autowired ChatRoomListCacheService chatRoomListCacheService;
    @Autowired ChatMemberHardDeleteCleanupService chatMemberHardDeleteCleanupService;
    @Autowired ApplicationEventPublisher applicationEventPublisher;

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

        memberPartyRepository.save(MemberFixture.createMemberParty(party, member, Role.PARTY_MANAGER));
        memberPartyRepository.save(MemberFixture.createMemberParty(party, otherMember, Role.PARTY_MEMBER));

        partyChatRoom = chatRoomRepository.save(ChatFixture.createPartyChatRoom(party));
        directChatRoom = chatRoomRepository.save(ChatFixture.createDirectChatRoom());
    }

    @AfterEach
    void tearDown() {
        chatRoomListCacheService.evictAllLastMessages();
        messageReadStatusRepository.deleteAll();
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
    @DisplayName("ChatMessageRepository - 시스템 메시지 조회 쿼리")
    class ChatMessageRepositoryQueries {

        @Test
        @DisplayName("findRecentMessagesWithFiles는 sender가 null인 시스템 메시지를 반환한다")
        void findRecentMessagesWithFiles_returnsSystemMessageWithoutSender() {
            ChatMessage systemMessage = chatMessageRepository.saveAndFlush(
                    ChatFixture.createSystemMessage(partyChatRoom, "시스템 공지"));

            List<ChatMessage> messages = chatMessageRepository.findRecentMessagesWithFiles(
                    partyChatRoom.getId(),
                    PageRequest.of(0, 10)
            );

            assertThat(messages).extracting(ChatMessage::getId).contains(systemMessage.getId());
            ChatMessage foundMessage = messages.stream()
                    .filter(message -> message.getId().equals(systemMessage.getId()))
                    .findFirst()
                    .orElseThrow();
            assertThat(foundMessage.getSender()).isNull();
            assertThat(foundMessage.getType().name()).isEqualTo("SYSTEM");
            assertThat(foundMessage.getContent()).isEqualTo("시스템 공지");
        }

        @Test
        @DisplayName("findByRoomIdAndIdLessThanOrderByCreatedAtDesc는 sender가 null인 시스템 메시지를 반환한다")
        void findPreviousMessages_returnsSystemMessageWithoutSender() {
            ChatMessage olderMessage = chatMessageRepository.saveAndFlush(
                    ChatFixture.createSystemMessage(partyChatRoom, "이전 시스템 공지"));
            ChatMessage newerMessage = chatMessageRepository.saveAndFlush(
                    ChatFixture.createTextMessage(partyChatRoom, member, "기준 메시지"));

            List<ChatMessage> messages = chatMessageRepository.findByRoomIdAndIdLessThanOrderByCreatedAtDesc(
                    partyChatRoom.getId(),
                    newerMessage.getId() + 1,
                    PageRequest.of(0, 10)
            );

            assertThat(messages).extracting(ChatMessage::getId).contains(olderMessage.getId(), newerMessage.getId());
            ChatMessage foundSystemMessage = messages.stream()
                    .filter(message -> message.getId().equals(olderMessage.getId()))
                    .findFirst()
                    .orElseThrow();
            assertThat(foundSystemMessage.getSender()).isNull();
            assertThat(foundSystemMessage.getType().name()).isEqualTo("SYSTEM");
            assertThat(foundSystemMessage.getContent()).isEqualTo("이전 시스템 공지");
        }
    }

    @Nested
    @DisplayName("채팅 안 읽은 메시지 수 API")
    class GetUnreadCounts {

        @Test
        @DisplayName("200 - 안 읽은 메시지가 없으면 요약 API는 0과 hasUnread false를 반환한다")
        void getUnreadSummary_noUnreadMessages() throws Exception {
            SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

            mockMvc.perform(get("/api/chats/unread/summary"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalUnreadCount").value(0))
                    .andExpect(jsonPath("$.data.partyUnreadCount").value(0))
                    .andExpect(jsonPath("$.data.directUnreadCount").value(0))
                    .andExpect(jsonPath("$.data.hasUnread").value(false));
        }

        @Test
        @DisplayName("200 - 요약 API는 모임과 JOINED 개인 채팅 안읽음 수를 합산한다")
        void getUnreadSummary_sumsPartyAndDirectUnreadCounts() throws Exception {
            createUnreadCountScenario();
            SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

            mockMvc.perform(get("/api/chats/unread/summary"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalUnreadCount").value(3))
                    .andExpect(jsonPath("$.data.partyUnreadCount").value(2))
                    .andExpect(jsonPath("$.data.directUnreadCount").value(1))
                    .andExpect(jsonPath("$.data.hasUnread").value(true));
        }

        @Test
        @DisplayName("200 - 모임 채팅 안읽음 수 API는 모임 채팅만 합산한다")
        void getPartyUnreadCount_returnsPartyUnreadOnly() throws Exception {
            createUnreadCountScenario();
            SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

            mockMvc.perform(get("/api/chats/parties/unread-count"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.unreadCount").value(2))
                    .andExpect(jsonPath("$.data.hasUnread").value(true));
        }

        @Test
        @DisplayName("200 - 개인 채팅 안읽음 수 API는 JOINED 개인 채팅만 합산한다")
        void getDirectUnreadCount_returnsJoinedDirectUnreadOnly() throws Exception {
            createUnreadCountScenario();
            SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

            mockMvc.perform(get("/api/chats/direct/unread-count"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.unreadCount").value(1))
                    .andExpect(jsonPath("$.data.hasUnread").value(true));
        }
    }

    @Nested
    @DisplayName("GET /api/chats/parties - 모임 채팅방 목록 조회")
    class GetPartyChatRooms {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("200 - 인증된 회원은 자신의 모임 채팅방 목록을 조회할 수 있다")
            void getPartyChatRooms_success() throws Exception {
                chatRoomMemberRepository.save(ChatRoomMember.create(partyChatRoom, member));
                chatMessageRepository.save(
                        ChatFixture.createTextMessage(partyChatRoom, member, "최근 공지"));

                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/chats/parties")
                                .param("page", "0")
                                .param("size", "10"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.hasNext").value(false))
                        .andExpect(jsonPath("$.data.content", hasSize(1)))
                        .andExpect(jsonPath("$.data.content[0].chatRoomId").value(partyChatRoom.getId()))
                        .andExpect(jsonPath("$.data.content[0].partyId").value(party.getId()))
                        .andExpect(jsonPath("$.data.content[0].partyName").value("배드민턴 모임"))
                        .andExpect(jsonPath("$.data.content[0].memberCount").value(1))
                        .andExpect(jsonPath("$.data.content[0].unreadCount").value(0))
                        .andExpect(jsonPath("$.data.content[0].partyImgUrl").doesNotExist())
                        .andExpect(jsonPath("$.data.content[0].lastMessage.content").value("최근 공지"))
                        .andExpect(jsonPath("$.data.content[0].lastMessage.messageType").value("TEXT"))
                        .andExpect(jsonPath("$.data.content[0].lastMessage.timestamp").exists());
            }

            @Test
            @DisplayName("200 - 참여 중인 모임 채팅방이 없으면 빈 목록을 반환한다")
            void getPartyChatRooms_empty() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/chats/parties")
                                .param("page", "0")
                                .param("size", "10"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.hasNext").value(false))
                        .andExpect(jsonPath("$.data.content", hasSize(0)));
            }

            @Test
            @DisplayName("200 - 마지막 메시지가 더 최근인 채팅방이 목록의 위에 온다")
            void getPartyChatRooms_latestMessageRoomFirst() throws Exception {
                PartyAddr secondAddr = partyAddrRepository.save(PartyFixture.createPartyAddr("서울특별시", "송파구"));
                Party secondParty = partyRepository.save(PartyFixture.createParty("새 모임", member.getId(), secondAddr));
                memberPartyRepository.save(MemberFixture.createMemberParty(secondParty, member, Role.PARTY_MANAGER));

                ChatRoom secondPartyChatRoom = chatRoomRepository.save(ChatFixture.createPartyChatRoom(secondParty));

                chatRoomMemberRepository.save(ChatRoomMember.create(partyChatRoom, member));
                chatRoomMemberRepository.save(ChatRoomMember.create(secondPartyChatRoom, member));

                chatMessageRepository.save(ChatFixture.createTextMessage(partyChatRoom, member, "먼저 온 메시지"));
                chatMessageRepository.save(ChatFixture.createTextMessage(secondPartyChatRoom, member, "가장 최근 메시지"));

                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/chats/parties")
                                .param("page", "0")
                                .param("size", "10"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.content", hasSize(2)))
                        .andExpect(jsonPath("$.data.content[0].chatRoomId").value(secondPartyChatRoom.getId()))
                        .andExpect(jsonPath("$.data.content[0].partyId").value(secondParty.getId()))
                        .andExpect(jsonPath("$.data.content[0].partyName").value("새 모임"))
                        .andExpect(jsonPath("$.data.content[0].lastMessage.content").value("가장 최근 메시지"))
                        .andExpect(jsonPath("$.data.content[1].chatRoomId").value(partyChatRoom.getId()))
                        .andExpect(jsonPath("$.data.content[1].partyId").value(party.getId()))
                        .andExpect(jsonPath("$.data.content[1].partyName").value("배드민턴 모임"))
                        .andExpect(jsonPath("$.data.content[1].lastMessage.content").value("먼저 온 메시지"));
            }

            @Test
            @DisplayName("200 - unreadCount와 partyImgUrl이 실제 값으로 채워진다")
            void getPartyChatRooms_populatedUnreadCountAndPartyImgUrl() throws Exception {
                PartyAddr richAddr = partyAddrRepository.save(PartyFixture.createPartyAddr("서울특별시", "용산구"));
                Party richParty = PartyFixture.createParty("이미지 있는 모임", member.getId(), richAddr);
                ReflectionTestUtils.invokeMethod(richParty, "setPartyImg", PartyImg.create("party/test-image.png", richParty));
                richParty = partyRepository.saveAndFlush(richParty);

                memberPartyRepository.save(MemberFixture.createMemberParty(richParty, member, Role.PARTY_MANAGER));
                memberPartyRepository.save(MemberFixture.createMemberParty(richParty, otherMember, Role.PARTY_MEMBER));

                ChatRoom richPartyChatRoom = chatRoomRepository.save(ChatFixture.createPartyChatRoom(richParty));
                chatRoomMemberRepository.save(ChatRoomMember.create(richPartyChatRoom, member));

                ChatMessage firstMessage = chatMessageRepository.save(
                        ChatFixture.createTextMessage(richPartyChatRoom, otherMember, "첫 번째 메시지"));
                ChatMessage latestMessage = chatMessageRepository.save(
                        ChatFixture.createTextMessage(richPartyChatRoom, otherMember, "읽지 않은 최근 메시지"));

                messageReadStatusRepository.save(MessageReadStatus.createUnread(firstMessage.getId(), member.getId(), richPartyChatRoom.getId()));
                messageReadStatusRepository.save(MessageReadStatus.createUnread(latestMessage.getId(), member.getId(), richPartyChatRoom.getId()));

                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/chats/parties")
                                .param("page", "0")
                                .param("size", "10"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.hasNext").value(false))
                        .andExpect(jsonPath("$.data.content", hasSize(1)))
                        .andExpect(jsonPath("$.data.content[0].chatRoomId").value(richPartyChatRoom.getId()))
                        .andExpect(jsonPath("$.data.content[0].unreadCount").value(2))
                        .andExpect(jsonPath("$.data.content[0].partyImgUrl").value("https://storage.googleapis.com/test-bucket/party/test-image.png"))
                        .andExpect(jsonPath("$.data.content[0].partyName").value("이미지 있는 모임"))
                        .andExpect(jsonPath("$.data.content[0].lastMessage.content").value("읽지 않은 최근 메시지"))
                        .andExpect(jsonPath("$.data.content[0].lastMessage.messageType").value("TEXT"));
            }
        }
    }

    @Nested
    @DisplayName("GET /api/chats/parties/search - 모임 채팅방 이름 검색")
    class SearchPartyChatRooms {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("200 - name query param으로 매칭된 모임 채팅방만 반환한다")
            void searchPartyChatRooms_filtersByName() throws Exception {
                PartyAddr matchAddr = partyAddrRepository.save(PartyFixture.createPartyAddr("서울특별시", "송파구"));
                Party matchingParty = partyRepository.save(PartyFixture.createParty("배드 모임", member.getId(), matchAddr));
                memberPartyRepository.save(MemberFixture.createMemberParty(matchingParty, member, Role.PARTY_MANAGER));

                PartyAddr nonMatchAddr = partyAddrRepository.save(PartyFixture.createPartyAddr("서울특별시", "마포구"));
                Party nonMatchingParty = partyRepository.save(PartyFixture.createParty("축구 모임", member.getId(), nonMatchAddr));
                memberPartyRepository.save(MemberFixture.createMemberParty(nonMatchingParty, member, Role.PARTY_MANAGER));

                ChatRoom matchingRoom = chatRoomRepository.save(ChatFixture.createPartyChatRoom(matchingParty));
                ChatRoom nonMatchingRoom = chatRoomRepository.save(ChatFixture.createPartyChatRoom(nonMatchingParty));

                chatRoomMemberRepository.save(ChatRoomMember.create(matchingRoom, member));
                chatRoomMemberRepository.save(ChatRoomMember.create(nonMatchingRoom, member));

                chatMessageRepository.save(ChatFixture.createTextMessage(matchingRoom, member, "배드 공지"));
                chatMessageRepository.save(ChatFixture.createTextMessage(nonMatchingRoom, member, "축구 공지"));

                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/chats/parties/search")
                                .param("name", "배드")
                                .param("page", "0")
                                .param("size", "10"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.hasNext").value(false))
                        .andExpect(jsonPath("$.data.content", hasSize(1)))
                        .andExpect(jsonPath("$.data.content[0].chatRoomId").value(matchingRoom.getId()))
                        .andExpect(jsonPath("$.data.content[0].partyId").value(matchingParty.getId()))
                        .andExpect(jsonPath("$.data.content[0].partyName").value("배드 모임"))
                        .andExpect(jsonPath("$.data.content[0].lastMessage.content").value("배드 공지"));
            }

            @Test
            @DisplayName("200 - 여러 검색 결과가 있으면 최근 메시지 방이 먼저 온다")
            void searchPartyChatRooms_latestMessageRoomFirst() throws Exception {
                PartyAddr secondAddr = partyAddrRepository.save(PartyFixture.createPartyAddr("서울특별시", "송파구"));
                Party secondMatchingParty = partyRepository.save(PartyFixture.createParty("배드 새 모임", member.getId(), secondAddr));
                memberPartyRepository.save(MemberFixture.createMemberParty(secondMatchingParty, member, Role.PARTY_MANAGER));

                PartyAddr nonMatchAddr = partyAddrRepository.save(PartyFixture.createPartyAddr("서울특별시", "용산구"));
                Party nonMatchingParty = partyRepository.save(PartyFixture.createParty("농구 모임", member.getId(), nonMatchAddr));
                memberPartyRepository.save(MemberFixture.createMemberParty(nonMatchingParty, member, Role.PARTY_MANAGER));

                ChatRoom secondMatchingRoom = chatRoomRepository.save(ChatFixture.createPartyChatRoom(secondMatchingParty));
                ChatRoom nonMatchingRoom = chatRoomRepository.save(ChatFixture.createPartyChatRoom(nonMatchingParty));

                chatRoomMemberRepository.save(ChatRoomMember.create(partyChatRoom, member));
                chatRoomMemberRepository.save(ChatRoomMember.create(secondMatchingRoom, member));
                chatRoomMemberRepository.save(ChatRoomMember.create(nonMatchingRoom, member));

                chatMessageRepository.save(ChatFixture.createTextMessage(partyChatRoom, member, "먼저 온 배드 메시지"));
                chatMessageRepository.save(ChatFixture.createTextMessage(secondMatchingRoom, member, "가장 최근 배드 메시지"));
                chatMessageRepository.save(ChatFixture.createTextMessage(nonMatchingRoom, member, "비매칭 메시지"));

                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/chats/parties/search")
                                .param("name", "배드")
                                .param("page", "0")
                                .param("size", "10"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.hasNext").value(false))
                        .andExpect(jsonPath("$.data.content", hasSize(2)))
                        .andExpect(jsonPath("$.data.content[0].chatRoomId").value(secondMatchingRoom.getId()))
                        .andExpect(jsonPath("$.data.content[0].partyId").value(secondMatchingParty.getId()))
                        .andExpect(jsonPath("$.data.content[0].partyName").value("배드 새 모임"))
                        .andExpect(jsonPath("$.data.content[0].lastMessage.content").value("가장 최근 배드 메시지"))
                        .andExpect(jsonPath("$.data.content[1].chatRoomId").value(partyChatRoom.getId()))
                        .andExpect(jsonPath("$.data.content[1].partyId").value(party.getId()))
                        .andExpect(jsonPath("$.data.content[1].partyName").value("배드민턴 모임"))
                        .andExpect(jsonPath("$.data.content[1].lastMessage.content").value("먼저 온 배드 메시지"));
            }

            @Test
            @DisplayName("200 - 검색 결과가 없으면 빈 목록과 hasNext false를 반환한다")
            void searchPartyChatRooms_empty() throws Exception {
                chatRoomMemberRepository.save(ChatRoomMember.create(partyChatRoom, member));
                chatMessageRepository.save(ChatFixture.createTextMessage(partyChatRoom, member, "배드민턴 공지"));

                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/chats/parties/search")
                                .param("name", "농구")
                                .param("page", "0")
                                .param("size", "10"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.hasNext").value(false))
                        .andExpect(jsonPath("$.data.content", hasSize(0)));
            }
        }
    }

    @Nested
    @DisplayName("POST /api/chats/direct - 개인 채팅방 생성 및 참여")
    class CreateDirectChatRoom {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("200 - 새로운 개인 채팅방을 생성하고 요청자는 JOINED, 상대방은 PENDING 상태로 저장한다")
            void createDirectChatRoom_success() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(post("/api/chats/direct")
                                .param("targetMemberId", otherMember.getId().toString()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.code").value(CommonSuccessCode.CREATED.getCode()))
                        .andExpect(jsonPath("$.message").value(CommonSuccessCode.CREATED.getMessage()))
                        .andExpect(jsonPath("$.data.chatRoomId").isNumber())
                        .andExpect(jsonPath("$.data.displayName").value(otherMember.getMemberName()))
                        .andExpect(jsonPath("$.data.createdAt").exists())
                        .andExpect(jsonPath("$.data.members", hasSize(2)))
                        .andExpect(jsonPath("$.data.members[*].memberName",
                                containsInAnyOrder(member.getMemberName(), otherMember.getMemberName())));
            }

            @Test
            @DisplayName("200 - 이미 존재하는 개인 채팅방이 있으면 새로 만들지 않고 기존 채팅방을 반환한다")
            void createDirectChatRoom_returnsExistingRoom() throws Exception {
                chatRoomMemberRepository.save(ChatRoomMember.createJoined(directChatRoom, member, otherMember.getMemberName()));
                chatRoomMemberRepository.save(ChatRoomMember.createPending(directChatRoom, otherMember, member.getMemberName()));

                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(post("/api/chats/direct")
                                .param("targetMemberId", otherMember.getId().toString()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.code").value(CommonSuccessCode.CREATED.getCode()))
                        .andExpect(jsonPath("$.message").value(CommonSuccessCode.CREATED.getMessage()))
                        .andExpect(jsonPath("$.data.chatRoomId").value(directChatRoom.getId()))
                        .andExpect(jsonPath("$.data.displayName").value(otherMember.getMemberName()))
                        .andExpect(jsonPath("$.data.createdAt").exists())
                        .andExpect(jsonPath("$.data.members", hasSize(2)))
                        .andExpect(jsonPath("$.data.members[*].memberName",
                                containsInAnyOrder(member.getMemberName(), otherMember.getMemberName())));
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("400 - 자기 자신을 대상으로 요청하면 CANNOT_CHAT_WITH_SELF 에러를 반환한다")
            void fail_cannotChatWithSelf() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(post("/api/chats/direct")
                                .param("targetMemberId", member.getId().toString()))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.code").value(ChatErrorCode.CANNOT_CHAT_WITH_SELF.getCode()))
                        .andExpect(jsonPath("$.message").value(ChatErrorCode.CANNOT_CHAT_WITH_SELF.getMessage()))
                        .andExpect(jsonPath("$.data").doesNotExist());
            }

            @Test
            @DisplayName("404 - 존재하지 않는 상대 회원이면 MEMBER_NOT_FOUND 에러를 반환한다")
            void fail_targetMemberNotFound() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(post("/api/chats/direct")
                                .param("targetMemberId", "999999"))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.code").value(MemberErrorCode.MEMBER_NOT_FOUND.getCode()))
                        .andExpect(jsonPath("$.message").value(MemberErrorCode.MEMBER_NOT_FOUND.getMessage()))
                        .andExpect(jsonPath("$.data").doesNotExist());
            }
        }
    }

    @Nested
    @DisplayName("GET /api/chats/direct - 개인 채팅방 목록 조회")
    class GetDirectChatRooms {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("200 - 최신 메시지 순으로 정렬된 개인 채팅방 목록과 상세 필드를 반환한다")
            void getDirectChatRooms_success() throws Exception {
                Member withdrawnCounterPart = memberRepository.save(
                        MemberFixture.createWithdrawnMember("탈퇴한사용자", "탈퇴", 3003L));

                Member latestCounterPart = memberRepository.save(
                        MemberFixture.createMember("이영희", Gender.FEMALE, Level.C, 4004L));
                latestCounterPart.updateProfileImg(ProfileImg.builder()
                        .imgKey("member/direct-profile.png")
                        .build());
                latestCounterPart = memberRepository.saveAndFlush(latestCounterPart);

                chatRoomMemberRepository.save(ChatRoomMember.createJoined(directChatRoom, member, "예전 대화"));
                chatRoomMemberRepository.save(ChatRoomMember.createJoined(directChatRoom, withdrawnCounterPart, member.getMemberName()));
                chatMessageRepository.save(
                        ChatFixture.createTextMessage(directChatRoom, withdrawnCounterPart, "먼저 온 메시지"));

                ChatRoom latestRoom = chatRoomRepository.save(ChatFixture.createDirectChatRoom());
                chatRoomMemberRepository.save(ChatRoomMember.createJoined(latestRoom, member, "영희 채팅"));
                chatRoomMemberRepository.save(ChatRoomMember.createJoined(latestRoom, latestCounterPart, member.getMemberName()));
                ChatMessage unreadMessage = chatMessageRepository.save(
                        ChatFixture.createTextMessage(latestRoom, latestCounterPart, "읽지 않은 메시지"));
                ChatMessage latestMessage = chatMessageRepository.save(
                        ChatFixture.createTextMessage(latestRoom, latestCounterPart, "가장 최근 메시지"));
                messageReadStatusRepository.save(MessageReadStatus.createUnread(unreadMessage.getId(), member.getId(), latestRoom.getId()));
                messageReadStatusRepository.save(MessageReadStatus.createUnread(latestMessage.getId(), member.getId(), latestRoom.getId()));

                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/chats/direct")
                                .param("page", "0")
                                .param("size", "10"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.code").value(CommonSuccessCode.OK.getCode()))
                        .andExpect(jsonPath("$.message").value(CommonSuccessCode.OK.getMessage()))
                        .andExpect(jsonPath("$.data.hasNext").value(false))
                        .andExpect(jsonPath("$.data.content", hasSize(2)))
                        .andExpect(jsonPath("$.data.content[0].chatRoomId").value(latestRoom.getId()))
                        .andExpect(jsonPath("$.data.content[0].displayName").value("영희 채팅"))
                        .andExpect(jsonPath("$.data.content[0].profileImgUrl").value("https://storage.googleapis.com/test-bucket/member/direct-profile.png"))
                        .andExpect(jsonPath("$.data.content[0].isWithdrawn").value(false))
                        .andExpect(jsonPath("$.data.content[0].unreadCount").value(2))
                        .andExpect(jsonPath("$.data.content[0].lastMessage.content").value("가장 최근 메시지"))
                        .andExpect(jsonPath("$.data.content[0].lastMessage.messageType").value("TEXT"))
                        .andExpect(jsonPath("$.data.content[0].lastMessage.timestamp").exists())
                        .andExpect(jsonPath("$.data.content[1].chatRoomId").value(directChatRoom.getId()))
                        .andExpect(jsonPath("$.data.content[1].displayName").value("알 수 없는 사용자"))
                        .andExpect(jsonPath("$.data.content[1].profileImgUrl").value(nullValue()))
                        .andExpect(jsonPath("$.data.content[1].isWithdrawn").value(true))
                        .andExpect(jsonPath("$.data.content[1].unreadCount").value(0))
                        .andExpect(jsonPath("$.data.content[1].lastMessage.content").value("먼저 온 메시지"))
                        .andExpect(jsonPath("$.data.content[1].lastMessage.messageType").value("TEXT"))
                        .andExpect(jsonPath("$.data.content[1].lastMessage.timestamp").exists());
            }

            @Test
            @DisplayName("200 - 현재 사용자가 JOINED 상태인 개인 채팅방만 반환한다")
            void getDirectChatRooms_filtersPendingMemberships() throws Exception {
                Member latestCounterPart = memberRepository.save(
                        MemberFixture.createMember("이영희", Gender.FEMALE, Level.C, 3003L));
                Member pendingCounterPart = memberRepository.save(
                        MemberFixture.createMember("박민수", Gender.MALE, Level.D, 4004L));

                chatRoomMemberRepository.save(ChatRoomMember.createJoined(directChatRoom, member, "철수 채팅"));
                chatRoomMemberRepository.save(ChatRoomMember.createJoined(directChatRoom, otherMember, member.getMemberName()));
                chatMessageRepository.save(ChatFixture.createTextMessage(directChatRoom, otherMember, "먼저 온 메시지"));

                ChatRoom latestJoinedRoom = chatRoomRepository.save(ChatFixture.createDirectChatRoom());
                chatRoomMemberRepository.save(ChatRoomMember.createJoined(latestJoinedRoom, member, "영희 채팅"));
                chatRoomMemberRepository.save(ChatRoomMember.createJoined(latestJoinedRoom, latestCounterPart, member.getMemberName()));
                chatMessageRepository.save(ChatFixture.createTextMessage(latestJoinedRoom, latestCounterPart, "최신 JOINED 메시지"));

                ChatRoom pendingRoom = chatRoomRepository.save(ChatFixture.createDirectChatRoom());
                chatRoomMemberRepository.save(ChatRoomMember.createPending(pendingRoom, member, "보이면 안 되는 방"));
                chatRoomMemberRepository.save(ChatRoomMember.createJoined(pendingRoom, pendingCounterPart, member.getMemberName()));
                chatMessageRepository.save(ChatFixture.createTextMessage(pendingRoom, pendingCounterPart, "가장 최신이지만 제외되어야 하는 메시지"));

                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/chats/direct")
                                .param("page", "0")
                                .param("size", "10"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.code").value(CommonSuccessCode.OK.getCode()))
                        .andExpect(jsonPath("$.message").value(CommonSuccessCode.OK.getMessage()))
                        .andExpect(jsonPath("$.data.hasNext").value(false))
                        .andExpect(jsonPath("$.data.content", hasSize(2)))
                        .andExpect(jsonPath("$.data.content[0].chatRoomId").value(latestJoinedRoom.getId()))
                        .andExpect(jsonPath("$.data.content[1].chatRoomId").value(directChatRoom.getId()));
            }

            @Test
            @DisplayName("200 - hard delete 전처리 후 삭제된 상대방은 알 수 없는 사용자로 조회된다")
            void hardDeletedCounterPart_isMappedToUnknownUser() throws Exception {
                Member target = memberRepository.save(
                        MemberFixture.createWithdrawnMember("삭제대상", "삭제", 5005L));

                chatRoomMemberRepository.save(ChatRoomMember.createJoined(directChatRoom, member, target.getMemberName()));
                chatRoomMemberRepository.save(ChatRoomMember.createJoined(directChatRoom, target, member.getMemberName()));
                ChatMessage targetMessage = chatMessageRepository.save(
                        ChatFixture.createTextMessage(directChatRoom, target, "삭제 대상 메시지"));
                messageReadStatusRepository.save(MessageReadStatus.createUnread(targetMessage.getId(), member.getId(), directChatRoom.getId()));
                messageReadStatusRepository.save(MessageReadStatus.createRead(targetMessage.getId(), target.getId(), directChatRoom.getId()));

                ChatMemberHardDeleteCleanupService.Result cleanupResult =
                        chatMemberHardDeleteCleanupService.prepareMemberHardDelete(target.getId());
                memberRepository.delete(target);
                memberRepository.flush();

                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/chats/direct")
                                .param("page", "0")
                                .param("size", "10"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.content", hasSize(1)))
                        .andExpect(jsonPath("$.data.content[0].displayName").value("알 수 없는 사용자"))
                        .andExpect(jsonPath("$.data.content[0].profileImgUrl").value(nullValue()))
                        .andExpect(jsonPath("$.data.content[0].isWithdrawn").value(true))
                        .andExpect(jsonPath("$.data.content[0].lastMessage.content").value("삭제 대상 메시지"));

                mockMvc.perform(get("/api/chats/rooms/{roomId}", directChatRoom.getId()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.chatRoomInfo.displayName").value("알 수 없는 사용자"))
                        .andExpect(jsonPath("$.data.chatRoomInfo.profileImageUrl").value(nullValue()))
                        .andExpect(jsonPath("$.data.chatRoomInfo.isCounterPartWithdrawn").value(true))
                        .andExpect(jsonPath("$.data.messages[0].senderId").value(nullValue()))
                        .andExpect(jsonPath("$.data.messages[0].senderName").value("알 수 없는 사용자"))
                        .andExpect(jsonPath("$.data.messages[0].isSenderWithdrawn").value(true));

                assertThat(cleanupResult.clearedMessages()).isEqualTo(1);
                assertThat(cleanupResult.clearedChatRoomMembers()).isEqualTo(1);
                assertThat(cleanupResult.deletedReadStatuses()).isEqualTo(1);
                assertThat(messageReadStatusRepository.findAll())
                        .noneMatch(status -> target.getId().equals(status.getMemberId()));
            }
        }
    }

    @Nested
    @DisplayName("GET /api/chats/direct/search - 개인 채팅방 이름 검색")
    class SearchDirectChatRooms {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("200 - 현재 사용자 membership displayName과 매칭되는 direct 채팅방만 반환한다")
            void searchDirectChatRooms_filtersByCurrentMembershipDisplayName() throws Exception {
                Member hiddenCounterPart = memberRepository.save(
                        MemberFixture.createMember("박민수", Gender.MALE, Level.C, 3003L));
                Member nonMatchingCounterPart = memberRepository.save(
                        MemberFixture.createMember("최유리", Gender.FEMALE, Level.B, 4004L));

                chatRoomMemberRepository.save(ChatRoomMember.createJoined(directChatRoom, member, "영희 채팅"));
                chatRoomMemberRepository.save(ChatRoomMember.createJoined(directChatRoom, otherMember, member.getMemberName()));
                chatMessageRepository.save(ChatFixture.createTextMessage(directChatRoom, otherMember, "영희와의 최근 메시지"));

                ChatRoom misleadingRoom = chatRoomRepository.save(ChatFixture.createDirectChatRoom());
                chatRoomMemberRepository.save(ChatRoomMember.createJoined(misleadingRoom, member, "숨김 대화"));
                chatRoomMemberRepository.save(ChatRoomMember.createJoined(misleadingRoom, hiddenCounterPart, "영희 채팅"));
                chatMessageRepository.save(ChatFixture.createTextMessage(misleadingRoom, hiddenCounterPart, "상대방 displayName만 일치"));

                ChatRoom nonMatchingRoom = chatRoomRepository.save(ChatFixture.createDirectChatRoom());
                chatRoomMemberRepository.save(ChatRoomMember.createJoined(nonMatchingRoom, member, "민수 채팅"));
                chatRoomMemberRepository.save(ChatRoomMember.createJoined(nonMatchingRoom, nonMatchingCounterPart, member.getMemberName()));
                chatMessageRepository.save(ChatFixture.createTextMessage(nonMatchingRoom, nonMatchingCounterPart, "검색어 미포함 메시지"));

                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/chats/direct/search")
                                .param("name", "영희")
                                .param("page", "0")
                                .param("size", "10"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.code").value(CommonSuccessCode.OK.getCode()))
                        .andExpect(jsonPath("$.message").value(CommonSuccessCode.OK.getMessage()))
                        .andExpect(jsonPath("$.data.hasNext").value(false))
                        .andExpect(jsonPath("$.data.content", hasSize(1)))
                        .andExpect(jsonPath("$.data.content[0].chatRoomId").value(directChatRoom.getId()))
                        .andExpect(jsonPath("$.data.content[0].displayName").value("영희 채팅"))
                        .andExpect(jsonPath("$.data.content[0].lastMessage.content").value("영희와의 최근 메시지"));
            }

            @Test
            @DisplayName("200 - 회원 탈퇴 이벤트로 익명화된 direct 채팅방은 삭제 전 이름으로 검색되지 않는다")
            void searchDirectChatRooms_excludesWithdrawnMemberNameAfterAnonymizationEvent() throws Exception {
                Member target = memberRepository.save(
                        MemberFixture.createMember("삭제대상", Gender.FEMALE, Level.C, 5005L));

                chatRoomMemberRepository.save(ChatRoomMember.createJoined(directChatRoom, member, target.getMemberName()));
                chatRoomMemberRepository.save(ChatRoomMember.createJoined(directChatRoom, target, member.getMemberName()));
                chatMessageRepository.save(ChatFixture.createTextMessage(directChatRoom, target, "탈퇴 전 메시지"));

                target.withdraw();
                memberRepository.saveAndFlush(target);
                applicationEventPublisher.publishEvent(MemberWithdrawnEvent.withdrawn(target.getId()));

                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/chats/direct/search")
                                .param("name", target.getMemberName())
                                .param("page", "0")
                                .param("size", "10"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.content", hasSize(0)))
                        .andExpect(jsonPath("$.data.hasNext").value(false));
            }

            @Test
            @DisplayName("200 - DIRECT가 아닌 채팅방은 검색 결과에서 제외된다")
            void searchDirectChatRooms_excludesNonDirectRooms() throws Exception {
                chatRoomMemberRepository.save(ChatRoomMember.createJoined(directChatRoom, member, "영희 채팅"));
                chatRoomMemberRepository.save(ChatRoomMember.createJoined(directChatRoom, otherMember, member.getMemberName()));
                chatMessageRepository.save(ChatFixture.createTextMessage(directChatRoom, otherMember, "direct 메시지"));

                chatRoomMemberRepository.save(ChatRoomMember.createJoined(partyChatRoom, member, "영희 채팅"));
                chatRoomMemberRepository.save(ChatRoomMember.createJoined(partyChatRoom, otherMember, member.getMemberName()));
                chatMessageRepository.save(ChatFixture.createTextMessage(partyChatRoom, otherMember, "party 메시지"));

                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/chats/direct/search")
                                .param("name", "영희")
                                .param("page", "0")
                                .param("size", "10"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.hasNext").value(false))
                        .andExpect(jsonPath("$.data.content", hasSize(1)))
                        .andExpect(jsonPath("$.data.content[0].chatRoomId").value(directChatRoom.getId()))
                        .andExpect(jsonPath("$.data.content[0].displayName").value("영희 채팅"));
            }

            @Test
            @DisplayName("200 - 현재 사용자가 JOINED 상태인 개인 채팅방만 검색된다")
            void searchDirectChatRooms_filtersPendingMemberships() throws Exception {
                Member joinedCounterPart = memberRepository.save(
                        MemberFixture.createMember("이영희", Gender.FEMALE, Level.C, 3003L));
                Member pendingCounterPart = memberRepository.save(
                        MemberFixture.createMember("박민수", Gender.MALE, Level.D, 4004L));

                chatRoomMemberRepository.save(ChatRoomMember.createJoined(directChatRoom, member, "영희 채팅"));
                chatRoomMemberRepository.save(ChatRoomMember.createJoined(directChatRoom, joinedCounterPart, member.getMemberName()));
                chatMessageRepository.save(ChatFixture.createTextMessage(directChatRoom, joinedCounterPart, "JOINED 방 메시지"));

                ChatRoom pendingRoom = chatRoomRepository.save(ChatFixture.createDirectChatRoom());
                chatRoomMemberRepository.save(ChatRoomMember.createPending(pendingRoom, member, "영희 보류 대화"));
                chatRoomMemberRepository.save(ChatRoomMember.createJoined(pendingRoom, pendingCounterPart, member.getMemberName()));
                chatMessageRepository.save(ChatFixture.createTextMessage(pendingRoom, pendingCounterPart, "PENDING 방 메시지"));

                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/chats/direct/search")
                                .param("name", "영희")
                                .param("page", "0")
                                .param("size", "10"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.hasNext").value(false))
                        .andExpect(jsonPath("$.data.content", hasSize(1)))
                        .andExpect(jsonPath("$.data.content[0].chatRoomId").value(directChatRoom.getId()))
                        .andExpect(jsonPath("$.data.content[0].displayName").value("영희 채팅"));
            }

            @Test
            @DisplayName("200 - 여러 검색 결과가 있으면 최신 메시지 순으로 반환한다")
            void searchDirectChatRooms_latestMessageRoomFirst() throws Exception {
                Member latestCounterPart = memberRepository.save(
                        MemberFixture.createMember("이영희", Gender.FEMALE, Level.C, 3003L));

                chatRoomMemberRepository.save(ChatRoomMember.createJoined(directChatRoom, member, "영희 예전 채팅"));
                chatRoomMemberRepository.save(ChatRoomMember.createJoined(directChatRoom, otherMember, member.getMemberName()));
                chatMessageRepository.save(ChatFixture.createTextMessage(directChatRoom, otherMember, "먼저 온 영희 메시지"));

                ChatRoom latestRoom = chatRoomRepository.save(ChatFixture.createDirectChatRoom());
                chatRoomMemberRepository.save(ChatRoomMember.createJoined(latestRoom, member, "영희 최신 채팅"));
                chatRoomMemberRepository.save(ChatRoomMember.createJoined(latestRoom, latestCounterPart, member.getMemberName()));
                chatMessageRepository.save(ChatFixture.createTextMessage(latestRoom, latestCounterPart, "가장 최근 영희 메시지"));

                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/chats/direct/search")
                                .param("name", "영희")
                                .param("page", "0")
                                .param("size", "10"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.hasNext").value(false))
                        .andExpect(jsonPath("$.data.content", hasSize(2)))
                        .andExpect(jsonPath("$.data.content[0].chatRoomId").value(latestRoom.getId()))
                        .andExpect(jsonPath("$.data.content[0].displayName").value("영희 최신 채팅"))
                        .andExpect(jsonPath("$.data.content[1].chatRoomId").value(directChatRoom.getId()))
                        .andExpect(jsonPath("$.data.content[1].displayName").value("영희 예전 채팅"));
            }

            @Test
            @DisplayName("200 - paging 파라미터에 따라 slice와 hasNext를 반환한다")
            void searchDirectChatRooms_supportsPaging() throws Exception {
                Member firstCounterPart = memberRepository.save(
                        MemberFixture.createMember("이영희", Gender.FEMALE, Level.C, 3003L));
                Member secondCounterPart = memberRepository.save(
                        MemberFixture.createMember("김영희", Gender.FEMALE, Level.B, 4004L));
                Member thirdCounterPart = memberRepository.save(
                        MemberFixture.createMember("박영희", Gender.FEMALE, Level.A, 5005L));

                chatRoomMemberRepository.save(ChatRoomMember.createJoined(directChatRoom, member, "영희 첫 채팅"));
                chatRoomMemberRepository.save(ChatRoomMember.createJoined(directChatRoom, firstCounterPart, member.getMemberName()));
                chatMessageRepository.save(ChatFixture.createTextMessage(directChatRoom, firstCounterPart, "첫 번째 영희 메시지"));

                ChatRoom secondRoom = chatRoomRepository.save(ChatFixture.createDirectChatRoom());
                chatRoomMemberRepository.save(ChatRoomMember.createJoined(secondRoom, member, "영희 두번째 채팅"));
                chatRoomMemberRepository.save(ChatRoomMember.createJoined(secondRoom, secondCounterPart, member.getMemberName()));
                chatMessageRepository.save(ChatFixture.createTextMessage(secondRoom, secondCounterPart, "두 번째 영희 메시지"));

                ChatRoom thirdRoom = chatRoomRepository.save(ChatFixture.createDirectChatRoom());
                chatRoomMemberRepository.save(ChatRoomMember.createJoined(thirdRoom, member, "영희 세번째 채팅"));
                chatRoomMemberRepository.save(ChatRoomMember.createJoined(thirdRoom, thirdCounterPart, member.getMemberName()));
                chatMessageRepository.save(ChatFixture.createTextMessage(thirdRoom, thirdCounterPart, "세 번째 영희 메시지"));

                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/chats/direct/search")
                                .param("name", "영희")
                                .param("page", "0")
                                .param("size", "2"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.hasNext").value(true))
                        .andExpect(jsonPath("$.data.content", hasSize(2)))
                        .andExpect(jsonPath("$.data.content[0].chatRoomId").value(thirdRoom.getId()))
                        .andExpect(jsonPath("$.data.content[1].chatRoomId").value(secondRoom.getId()));

                mockMvc.perform(get("/api/chats/direct/search")
                                .param("name", "영희")
                                .param("page", "1")
                                .param("size", "2"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.hasNext").value(false))
                        .andExpect(jsonPath("$.data.content", hasSize(1)))
                        .andExpect(jsonPath("$.data.content[0].chatRoomId").value(directChatRoom.getId()));
            }

            @Test
            @DisplayName("200 - 검색 결과가 없으면 빈 목록과 hasNext false를 반환한다")
            void searchDirectChatRooms_empty() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/chats/direct/search")
                                .param("name", "없는검색어")
                                .param("page", "0")
                                .param("size", "10"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.hasNext").value(false))
                        .andExpect(jsonPath("$.data.content", hasSize(0)));
            }

            @Test
            @DisplayName("200 - 빈 검색어면 현재 사용자가 JOINED인 개인 채팅방 전체를 최신순으로 반환한다")
            void searchDirectChatRooms_blankNameReturnsAllJoinedDirectRooms() throws Exception {
                Member latestCounterPart = memberRepository.save(
                        MemberFixture.createMember("김영희", Gender.FEMALE, Level.C, 3003L));
                Member pendingCounterPart = memberRepository.save(
                        MemberFixture.createMember("박민수", Gender.MALE, Level.D, 4004L));

                chatRoomMemberRepository.save(ChatRoomMember.createJoined(directChatRoom, member, "첫 번째 대화"));
                chatRoomMemberRepository.save(ChatRoomMember.createJoined(directChatRoom, otherMember, member.getMemberName()));
                chatMessageRepository.save(ChatFixture.createTextMessage(directChatRoom, otherMember, "먼저 온 메시지"));

                ChatRoom latestRoom = chatRoomRepository.save(ChatFixture.createDirectChatRoom());
                chatRoomMemberRepository.save(ChatRoomMember.createJoined(latestRoom, member, "두 번째 대화"));
                chatRoomMemberRepository.save(ChatRoomMember.createJoined(latestRoom, latestCounterPart, member.getMemberName()));
                chatMessageRepository.save(ChatFixture.createTextMessage(latestRoom, latestCounterPart, "가장 최근 메시지"));

                ChatRoom pendingRoom = chatRoomRepository.save(ChatFixture.createDirectChatRoom());
                chatRoomMemberRepository.save(ChatRoomMember.createPending(pendingRoom, member, "보이면 안 되는 대화"));
                chatRoomMemberRepository.save(ChatRoomMember.createJoined(pendingRoom, pendingCounterPart, member.getMemberName()));
                chatMessageRepository.save(ChatFixture.createTextMessage(pendingRoom, pendingCounterPart, "최신이지만 제외되어야 하는 메시지"));

                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/chats/direct/search")
                                .param("name", "")
                                .param("page", "0")
                                .param("size", "10"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data.hasNext").value(false))
                        .andExpect(jsonPath("$.data.content", hasSize(2)))
                        .andExpect(jsonPath("$.data.content[0].chatRoomId").value(latestRoom.getId()))
                        .andExpect(jsonPath("$.data.content[0].displayName").value("두 번째 대화"))
                        .andExpect(jsonPath("$.data.content[1].chatRoomId").value(directChatRoom.getId()))
                        .andExpect(jsonPath("$.data.content[1].displayName").value("첫 번째 대화"));
            }
        }
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
                        .andExpect(jsonPath("$.data.messages[0].images").isArray())
                        .andExpect(jsonPath("$.data.messages[0].images", hasSize(0)))
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
                        .andExpect(jsonPath("$.data.chatRoomInfo.displayName").value("알 수 없는 사용자"))
                        .andExpect(jsonPath("$.data.chatRoomInfo.profileImageUrl").value(nullValue()))
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
                        .andExpect(jsonPath("$.data.messages[0].senderName").value("알 수 없는 사용자"))
                        .andExpect(jsonPath("$.data.messages[0].senderProfileImageUrl").value(nullValue()))
                        .andExpect(jsonPath("$.data.messages[0].isSenderWithdrawn").value(true));
            }

            @Test
            @DisplayName("200 - sender가 null인 일반 메시지는 알 수 없는 사용자로 조회된다")
            void nullSenderTextMessage_isMappedToUnknownUser() throws Exception {
                chatRoomMemberRepository.save(ChatRoomMember.create(partyChatRoom, member));
                chatMessageRepository.save(
                        ChatMessage.create(partyChatRoom, null, "삭제된 사용자 메시지", MessageType.TEXT));

                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/chats/rooms/{roomId}", partyChatRoom.getId()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.messages[0].senderId").value(nullValue()))
                        .andExpect(jsonPath("$.data.messages[0].senderName").value("알 수 없는 사용자"))
                        .andExpect(jsonPath("$.data.messages[0].senderProfileImageUrl").value(nullValue()))
                        .andExpect(jsonPath("$.data.messages[0].isSenderWithdrawn").value(true))
                        .andExpect(jsonPath("$.data.messages[0].isMyMessage").value(false));
            }

            @Test
            @DisplayName("200 - 이미지 메시지 조회 시 images 필드에 파일 정보가 포함된다")
            void imageMessage_containsFileInfo() throws Exception {
                chatRoomMemberRepository.save(ChatRoomMember.create(partyChatRoom, member));

                ChatMessage imageMessage = chatMessageRepository.save(
                        ChatFixture.createImageMessage(partyChatRoom, member, java.util.List.of()));

                ChatMessageFile file1 = ChatMessageFile.create(imageMessage, "chat/img1.png", 1, "photo1.png", 1024L, "image/png");
                ChatMessageFile file2 = ChatMessageFile.create(imageMessage, "chat/img2.png", 2, "photo2.png", 2048L, "image/png");
                imageMessage.getChatMessageFiles().addAll(java.util.List.of(file1, file2));
                chatMessageRepository.saveAndFlush(imageMessage);

                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/chats/rooms/{roomId}", partyChatRoom.getId()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.messages[0].messageType").value("TEXT"))
                        .andExpect(jsonPath("$.data.messages[0].images").isArray())
                        .andExpect(jsonPath("$.data.messages[0].images", hasSize(2)))
                        .andExpect(jsonPath("$.data.messages[0].images[0].imgOrder").value(1))
                        .andExpect(jsonPath("$.data.messages[0].images[0].originalFileName").value("photo1.png"))
                        .andExpect(jsonPath("$.data.messages[0].images[0].fileSize").value(1024))
                        .andExpect(jsonPath("$.data.messages[0].images[0].fileType").value("image/png"))
                        .andExpect(jsonPath("$.data.messages[0].images[0].isEmoji").value(false))
                        .andExpect(jsonPath("$.data.messages[0].images[1].imgOrder").value(2))
                        .andExpect(jsonPath("$.data.messages[0].images[1].originalFileName").value("photo2.png"));
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

    @Nested
    @DisplayName("GET /api/chats/rooms/{roomId}/messages/previous - 과거 메시지 조회")
    class GetChatMessages {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("200 - 과거 메시지 조회 시 모든 응답 필드가 정확하게 반환된다")
            void getChatMessages_fullFieldValidation() throws Exception {
                chatRoomMemberRepository.save(ChatRoomMember.create(partyChatRoom, member));

                ChatMessage msg1 = chatMessageRepository.save(
                        ChatFixture.createTextMessage(partyChatRoom, member, "이전 메시지"));
                ChatMessage msg2 = chatMessageRepository.save(
                        ChatFixture.createTextMessage(partyChatRoom, member, "최근 메시지"));

                // msg2 이후부터 1개만 조회 -> msg2가 반환되고 hasNext=true, nextCursor=msg2.getId 확인
                Long cursor = msg2.getId() + 1;

                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/chats/rooms/{roomId}/messages/previous", partyChatRoom.getId())
                                .param("cursor", cursor.toString())
                                .param("size", "1"))
                        .andExpect(status().isOk())
                        // 1. 공통 필드 전수 검사
                        .andExpect(jsonPath("$.data.hasNext").value(true))
                        .andExpect(jsonPath("$.data.nextCursor").value(msg2.getId()))
                        // 2. messages 리스트 및 첫 번째 메시지 필드 전수 검사
                        .andExpect(jsonPath("$.data.messages").isArray())
                        .andExpect(jsonPath("$.data.messages", hasSize(1)))
                        .andExpect(jsonPath("$.data.messages[0].messageId").value(msg2.getId()))
                        .andExpect(jsonPath("$.data.messages[0].senderId").value(member.getId()))
                        .andExpect(jsonPath("$.data.messages[0].senderName").value("홍길동"))
                        .andExpect(jsonPath("$.data.messages[0].content").value("최근 메시지"))
                        .andExpect(jsonPath("$.data.messages[0].messageType").value("TEXT"))
                        .andExpect(jsonPath("$.data.messages[0].timestamp").exists())
                        .andExpect(jsonPath("$.data.messages[0].isMyMessage").value(true))
                        .andExpect(jsonPath("$.data.messages[0].isSenderWithdrawn").value(false))
                        .andExpect(jsonPath("$.data.messages[0].images").isArray())
                        .andExpect(jsonPath("$.data.messages[0].images", hasSize(0)));
            }

            @Test
            @DisplayName("200 - 메시지가 오래된 순으로 정렬되어 반환된다")
            void messages_areInChronologicalOrder() throws Exception {
                chatRoomMemberRepository.save(ChatRoomMember.create(partyChatRoom, member));

                ChatMessage msg1 = chatMessageRepository.save(
                        ChatFixture.createTextMessage(partyChatRoom, member, "첫 번째 메시지"));
                ChatMessage msg2 = chatMessageRepository.save(
                        ChatFixture.createTextMessage(partyChatRoom, member, "두 번째 메시지"));
                ChatMessage msg3 = chatMessageRepository.save(
                        ChatFixture.createTextMessage(partyChatRoom, member, "세 번째 메시지"));

                // cursor보다 id가 작은 메시지만 반환 → msg1, msg2, msg3 모두 cursor 미만이어야 하므로
                // cursor를 msg3 이후 값으로 설정
                Long cursor = msg3.getId() + 1;

                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/chats/rooms/{roomId}/messages/previous", partyChatRoom.getId())
                                .param("cursor", cursor.toString())
                                .param("size", "10"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.messages", hasSize(3)))
                        .andExpect(jsonPath("$.data.messages[0].messageId").value(msg1.getId()))
                        .andExpect(jsonPath("$.data.messages[1].messageId").value(msg2.getId()))
                        .andExpect(jsonPath("$.data.messages[2].messageId").value(msg3.getId()));
            }

            @Test
            @DisplayName("200 - size보다 메시지가 많으면 hasNext가 true이고 nextCursor가 설정된다")
            void hasNextTrue_whenMoreMessagesExist() throws Exception {
                chatRoomMemberRepository.save(ChatRoomMember.create(partyChatRoom, member));

                ChatMessage msg1 = chatMessageRepository.save(
                        ChatFixture.createTextMessage(partyChatRoom, member, "메시지1"));
                ChatMessage msg2 = chatMessageRepository.save(
                        ChatFixture.createTextMessage(partyChatRoom, member, "메시지2"));
                ChatMessage msg3 = chatMessageRepository.save(
                        ChatFixture.createTextMessage(partyChatRoom, member, "메시지3"));

                Long cursor = msg3.getId() + 1;

                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/chats/rooms/{roomId}/messages/previous", partyChatRoom.getId())
                                .param("cursor", cursor.toString())
                                .param("size", "2"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.hasNext").value(true))
                        .andExpect(jsonPath("$.data.nextCursor").isNumber())
                        .andExpect(jsonPath("$.data.messages", hasSize(2)));
            }

            @Test
            @DisplayName("200 - 메시지가 size 이하이면 hasNext가 false이고 nextCursor가 null이다")
            void hasNextFalse_whenNoMoreMessages() throws Exception {
                chatRoomMemberRepository.save(ChatRoomMember.create(partyChatRoom, member));

                ChatMessage msg1 = chatMessageRepository.save(
                        ChatFixture.createTextMessage(partyChatRoom, member, "메시지1"));
                ChatMessage msg2 = chatMessageRepository.save(
                        ChatFixture.createTextMessage(partyChatRoom, member, "메시지2"));

                Long cursor = msg2.getId() + 1;

                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/chats/rooms/{roomId}/messages/previous", partyChatRoom.getId())
                                .param("cursor", cursor.toString())
                                .param("size", "10"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.hasNext").value(false))
                        .andExpect(jsonPath("$.data.nextCursor").doesNotExist())
                        .andExpect(jsonPath("$.data.messages", hasSize(2)));
            }

            @Test
            @DisplayName("200 - cursor보다 id가 작은 메시지만 반환된다")
            void onlyMessagesBeforeCursorReturned() throws Exception {
                chatRoomMemberRepository.save(ChatRoomMember.create(partyChatRoom, member));

                ChatMessage msg1 = chatMessageRepository.save(
                        ChatFixture.createTextMessage(partyChatRoom, member, "오래된 메시지"));
                ChatMessage msg2 = chatMessageRepository.save(
                        ChatFixture.createTextMessage(partyChatRoom, member, "cursor 이후 메시지"));

                // msg2의 id를 cursor로 설정 → msg1만 반환되어야 함
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/chats/rooms/{roomId}/messages/previous", partyChatRoom.getId())
                                .param("cursor", msg2.getId().toString())
                                .param("size", "10"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.messages", hasSize(1)))
                        .andExpect(jsonPath("$.data.messages[0].messageId").value(msg1.getId()));
            }

            @Test
            @DisplayName("200 - 내가 보낸 메시지는 isMyMessage가 true이다")
            void myMessage_isMyMessageTrue() throws Exception {
                chatRoomMemberRepository.save(ChatRoomMember.create(partyChatRoom, member));

                ChatMessage myMsg = chatMessageRepository.save(
                        ChatFixture.createTextMessage(partyChatRoom, member, "내 메시지"));
                Long cursor = myMsg.getId() + 1;

                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/chats/rooms/{roomId}/messages/previous", partyChatRoom.getId())
                                .param("cursor", cursor.toString()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.messages[0].isMyMessage").value(true));
            }

            @Test
            @DisplayName("200 - 탈퇴한 사용자의 메시지는 isSenderWithdrawn이 true이다")
            void withdrawnSender_isSenderWithdrawnTrue() throws Exception {
                Member withdrawnMember = memberRepository.save(
                        MemberFixture.createWithdrawnMember("탈퇴한사용자", "탈퇴", 3003L));

                chatRoomMemberRepository.save(ChatRoomMember.create(partyChatRoom, member));
                chatRoomMemberRepository.save(ChatRoomMember.create(partyChatRoom, withdrawnMember));

                ChatMessage withdrawnMsg = chatMessageRepository.save(
                        ChatFixture.createTextMessage(partyChatRoom, withdrawnMember, "탈퇴자 메시지"));
                Long cursor = withdrawnMsg.getId() + 1;

                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/chats/rooms/{roomId}/messages/previous", partyChatRoom.getId())
                                .param("cursor", cursor.toString()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.messages[0].senderName").value("알 수 없는 사용자"))
                        .andExpect(jsonPath("$.data.messages[0].senderProfileImageUrl").value(nullValue()))
                        .andExpect(jsonPath("$.data.messages[0].isSenderWithdrawn").value(true));
            }

            @Test
            @DisplayName("200 - sender가 null인 일반 과거 메시지는 알 수 없는 사용자로 조회된다")
            void nullSenderPreviousMessage_isMappedToUnknownUser() throws Exception {
                chatRoomMemberRepository.save(ChatRoomMember.create(partyChatRoom, member));
                ChatMessage deletedSenderMessage = chatMessageRepository.save(
                        ChatMessage.create(partyChatRoom, null, "삭제된 사용자 메시지", MessageType.TEXT));
                Long cursor = deletedSenderMessage.getId() + 1;

                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/chats/rooms/{roomId}/messages/previous", partyChatRoom.getId())
                                .param("cursor", cursor.toString()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.messages[0].senderId").value(nullValue()))
                        .andExpect(jsonPath("$.data.messages[0].senderName").value("알 수 없는 사용자"))
                        .andExpect(jsonPath("$.data.messages[0].senderProfileImageUrl").value(nullValue()))
                        .andExpect(jsonPath("$.data.messages[0].isSenderWithdrawn").value(true))
                        .andExpect(jsonPath("$.data.messages[0].isMyMessage").value(false));
            }

            @Test
            @DisplayName("200 - 이미지 메시지 조회 시 images 필드에 파일 정보가 포함된다")
            void imageMessage_containsFileInfo() throws Exception {
                chatRoomMemberRepository.save(ChatRoomMember.create(partyChatRoom, member));

                ChatMessage imageMessage = chatMessageRepository.save(
                        ChatFixture.createImageMessage(partyChatRoom, member, java.util.List.of()));

                ChatMessageFile file1 = ChatMessageFile.create(imageMessage, "chat/img1.png", 1, "photo1.png", 1024L, "image/png");
                ChatMessageFile file2 = ChatMessageFile.create(imageMessage, "chat/img2.png", 2, "photo2.png", 2048L, "image/png");
                imageMessage.getChatMessageFiles().addAll(java.util.List.of(file1, file2));
                chatMessageRepository.saveAndFlush(imageMessage);

                Long cursor = imageMessage.getId() + 1;

                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/chats/rooms/{roomId}/messages/previous", partyChatRoom.getId())
                                .param("cursor", cursor.toString())
                                .param("size", "10"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.messages[0].messageType").value("TEXT"))
                        .andExpect(jsonPath("$.data.messages[0].images").isArray())
                        .andExpect(jsonPath("$.data.messages[0].images", hasSize(2)))
                        .andExpect(jsonPath("$.data.messages[0].images[0].imgOrder").value(1))
                        .andExpect(jsonPath("$.data.messages[0].images[0].originalFileName").value("photo1.png"))
                        .andExpect(jsonPath("$.data.messages[0].images[0].fileSize").value(1024))
                        .andExpect(jsonPath("$.data.messages[0].images[0].fileType").value("image/png"))
                        .andExpect(jsonPath("$.data.messages[0].images[0].isEmoji").value(false))
                        .andExpect(jsonPath("$.data.messages[0].images[1].imgOrder").value(2))
                        .andExpect(jsonPath("$.data.messages[0].images[1].originalFileName").value("photo2.png"));
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("400 - 채팅방 멤버가 아닌 사용자가 조회하면 CHAT_ROOM_ACCESS_DENIED 에러를 반환한다")
            void notChatRoomMember() throws Exception {
                chatRoomMemberRepository.save(ChatRoomMember.create(partyChatRoom, member));

                SecurityContextHelper.setAuthentication(otherMember.getId(), otherMember.getNickname());

                mockMvc.perform(get("/api/chats/rooms/{roomId}/messages/previous", partyChatRoom.getId())
                                .param("cursor", "100"))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED.getCode()))
                        .andExpect(jsonPath("$.message").value(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED.getMessage()));
            }
        }
    }

    @Nested
    @DisplayName("POST /api/chats/files/{fileId}/download-token - 파일 다운로드 토큰 발급")
    class IssueDownloadToken {

        private ChatMessageFile chatFile;

        @BeforeEach
        void setUpFile() {
            chatRoomMemberRepository.save(ChatRoomMember.create(partyChatRoom, member));
            ChatMessage message = chatMessageRepository.save(ChatFixture.createTextMessage(partyChatRoom, member, "이미지 첨부"));
            chatFile = ChatMessageFile.create(message, "test/key.webp", 1, "test.webp", 100L, "image/webp");
            message.getChatMessageFiles().add(chatFile);
            message = chatMessageRepository.saveAndFlush(message);
            chatFile = message.getChatMessageFiles().get(0);
        }

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("200 - 채팅방 권한이 있는 멤버는 토큰을 성공적으로 발급받는다")
            void success_issueToken() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(post("/api/chats/files/{fileId}/download-token", chatFile.getId()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.downloadToken").isString())
                        .andExpect(jsonPath("$.data.expiresAt").exists());
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("400 - 채팅방에 참여하지 않은 사용자가 토큰을 요청하면 접근 거부 에러를 반환한다")
            void fail_notRoomMember() throws Exception {
                SecurityContextHelper.setAuthentication(otherMember.getId(), otherMember.getNickname());

                mockMvc.perform(post("/api/chats/files/{fileId}/download-token", chatFile.getId()))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED.getCode()));
            }

            @Test
            @DisplayName("404 - 존재하지 않는 파일 ID로 요청하면 파일 없음 에러를 반환한다")
            void fail_fileNotFound() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(post("/api/chats/files/{fileId}/download-token", 99999L))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value(ChatErrorCode.FILE_NOT_FOUND.getCode()));
            }
        }
    }

    @Nested
    @DisplayName("GET /api/chats/files/{fileId}/download - 실제 파일 다운로드")
    class DownloadFile {

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("403 - 존재하지 않거나 유효하지 않은 토큰으로 접근하면 403 인증 에러 반환")
            void fail_invalidToken() throws Exception {
                chatRoomMemberRepository.save(ChatRoomMember.create(partyChatRoom, member));
                ChatMessage message = chatMessageRepository.save(ChatFixture.createTextMessage(partyChatRoom, member, "테스트"));
                ChatMessageFile chatFile = ChatMessageFile.create(message, "test/key.webp", 1, "test.webp", 100L, "image/webp");
                message.getChatMessageFiles().add(chatFile);
                message = chatMessageRepository.saveAndFlush(message);
                chatFile = message.getChatMessageFiles().get(0);

                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/chats/files/{fileId}/download", chatFile.getId()).param("token", "invalid-fake-token"))
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.code").value(ChatErrorCode.INVALID_DOWNLOAD_TOKEN.getCode()));
            }
        }
    }

    private void createUnreadCountScenario() {
        ChatRoomMember partyMembership = chatRoomMemberRepository.save(
                ChatRoomMember.create(partyChatRoom, member));
        chatRoomMemberRepository.save(ChatRoomMember.create(partyChatRoom, otherMember));

        ChatMessage alreadyReadPartyMessage = chatMessageRepository.save(
                ChatFixture.createTextMessage(partyChatRoom, otherMember, "이미 읽은 모임 메시지"));
        ChatMessage firstUnreadPartyMessage = chatMessageRepository.save(
                ChatFixture.createTextMessage(partyChatRoom, otherMember, "읽지 않은 모임 메시지 1"));
        ChatMessage secondUnreadPartyMessage = chatMessageRepository.save(
                ChatFixture.createTextMessage(partyChatRoom, otherMember, "읽지 않은 모임 메시지 2"));

        partyMembership.updateLastReadMessageId(alreadyReadPartyMessage.getId());
        chatRoomMemberRepository.saveAndFlush(partyMembership);

        chatRoomMemberRepository.save(ChatRoomMember.createJoined(directChatRoom, member, "철수 채팅"));
        chatRoomMemberRepository.save(ChatRoomMember.createJoined(directChatRoom, otherMember, member.getMemberName()));
        ChatMessage unreadDirectMessage = chatMessageRepository.save(
                ChatFixture.createTextMessage(directChatRoom, otherMember, "읽지 않은 개인 메시지"));

        ChatRoom pendingDirectRoom = chatRoomRepository.save(ChatFixture.createDirectChatRoom());
        chatRoomMemberRepository.save(ChatRoomMember.createPending(pendingDirectRoom, member, "보류 중인 채팅"));
        chatRoomMemberRepository.save(ChatRoomMember.createJoined(pendingDirectRoom, otherMember, member.getMemberName()));
        ChatMessage pendingDirectMessage = chatMessageRepository.save(
                ChatFixture.createTextMessage(pendingDirectRoom, otherMember, "제외되어야 하는 개인 메시지"));

        messageReadStatusRepository.saveAll(List.of(
                MessageReadStatus.createUnread(alreadyReadPartyMessage.getId(), member.getId(), partyChatRoom.getId()),
                MessageReadStatus.createUnread(firstUnreadPartyMessage.getId(), member.getId(), partyChatRoom.getId()),
                MessageReadStatus.createUnread(secondUnreadPartyMessage.getId(), member.getId(), partyChatRoom.getId()),
                MessageReadStatus.createUnread(secondUnreadPartyMessage.getId(), otherMember.getId(), partyChatRoom.getId()),
                MessageReadStatus.createUnread(unreadDirectMessage.getId(), member.getId(), directChatRoom.getId()),
                MessageReadStatus.createUnread(pendingDirectMessage.getId(), member.getId(), pendingDirectRoom.getId())
        ));
    }
}
