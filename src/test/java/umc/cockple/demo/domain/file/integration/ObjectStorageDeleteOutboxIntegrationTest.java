package umc.cockple.demo.domain.file.integration;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.chat.domain.ChatMessage;
import umc.cockple.demo.domain.chat.domain.ChatMessageFile;
import umc.cockple.demo.domain.chat.domain.ChatRoom;
import umc.cockple.demo.domain.chat.domain.ChatRoomMember;
import umc.cockple.demo.domain.chat.repository.ChatFileRepository;
import umc.cockple.demo.domain.chat.repository.ChatMessageRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomRepository;
import umc.cockple.demo.domain.file.domain.ObjectStorageDeleteOutbox;
import umc.cockple.demo.domain.file.enums.ObjectStorageDeleteSourceType;
import umc.cockple.demo.domain.file.enums.ObjectStorageDeleteStatus;
import umc.cockple.demo.domain.file.repository.ObjectStorageDeleteOutboxRepository;
import umc.cockple.demo.domain.file.service.ObjectStorageClient;
import umc.cockple.demo.domain.file.service.ObjectStorageDeleteOutboxProcessor;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberParty;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.domain.PartyAddr;
import umc.cockple.demo.domain.party.repository.PartyAddrRepository;
import umc.cockple.demo.domain.party.repository.PartyRepository;
import umc.cockple.demo.domain.party.service.PartyCommandService;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.global.enums.Role;
import umc.cockple.demo.support.IntegrationTestBase;
import umc.cockple.demo.support.fixture.ChatFixture;
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@Transactional
@Import(ObjectStorageDeleteOutboxIntegrationTest.TestObjectStorageConfig.class)
@DisplayName("ObjectStorageDeleteOutbox 통합 테스트")
class ObjectStorageDeleteOutboxIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private MemberPartyRepository memberPartyRepository;
    @Autowired
    private PartyAddrRepository partyAddrRepository;
    @Autowired
    private PartyRepository partyRepository;
    @Autowired
    private ChatRoomRepository chatRoomRepository;
    @Autowired
    private ChatRoomMemberRepository chatRoomMemberRepository;
    @Autowired
    private ChatMessageRepository chatMessageRepository;
    @Autowired
    private ChatFileRepository chatFileRepository;
    @Autowired
    private ObjectStorageDeleteOutboxRepository objectStorageDeleteOutboxRepository;
    @Autowired
    private ObjectStorageDeleteOutboxProcessor objectStorageDeleteOutboxProcessor;
    @Autowired
    private ObjectStorageClient objectStorageClient;
    @Autowired
    private PartyCommandService partyCommandService;

    @PersistenceContext
    private EntityManager entityManager;

    private Member owner;
    private Party party;
    private ChatRoom chatRoom;

    @BeforeEach
    void setUp() {
        reset(objectStorageClient);

        owner = memberRepository.save(MemberFixture.createMember("매니저", Gender.MALE, Level.A, 2001L));
        PartyAddr partyAddr = partyAddrRepository.save(PartyFixture.createPartyAddr("서울", "강남"));
        party = partyRepository.save(PartyFixture.createParty("삭제 테스트 모임", owner.getId(), partyAddr));
        memberPartyRepository.save(MemberParty.createOwner(owner, party));

        chatRoom = chatRoomRepository.save(ChatRoom.createPartyChatRoom(party));
        chatRoomMemberRepository.save(ChatRoomMember.create(chatRoom, owner));
    }

    @Test
    @DisplayName("모임 삭제 시 채팅 파일 object key를 outbox에 보존하고 processor가 삭제 완료 처리한다")
    void deleteParty_enqueuesObjectKey_thenProcessorMarksDone() {
        String objectKey = "chat/delete-target.jpg";
        ChatMessage message = chatMessageRepository.save(ChatFixture.createTextMessage(chatRoom, owner, "첨부 메시지"));
        ChatMessageFile file = ChatFixture.createChatMessageFile(message, objectKey, 0, "delete-target.jpg");
        message.getChatMessageFiles().add(file);
        chatMessageRepository.save(message);
        entityManager.flush();
        entityManager.clear();

        partyCommandService.deleteParty(party.getId(), owner.getId());
        entityManager.flush();
        entityManager.clear();

        List<ObjectStorageDeleteOutbox> outboxes = objectStorageDeleteOutboxRepository.findAll();
        assertThat(outboxes).hasSize(1);
        ObjectStorageDeleteOutbox outbox = outboxes.get(0);
        assertThat(outbox.getObjectKey()).isEqualTo(objectKey);
        assertThat(outbox.getSourceType()).isEqualTo(ObjectStorageDeleteSourceType.PARTY_CHAT_ROOM);
        assertThat(outbox.getSourceId()).isEqualTo(chatRoom.getId());
        assertThat(outbox.getStatus()).isEqualTo(ObjectStorageDeleteStatus.PENDING);
        assertThat(chatFileRepository.findObjectKeysByChatRoomId(chatRoom.getId())).isEmpty();

        objectStorageDeleteOutboxProcessor.processOne(outbox.getId());
        entityManager.flush();
        entityManager.clear();

        verify(objectStorageClient).delete(objectKey);
        ObjectStorageDeleteOutbox processedOutbox = objectStorageDeleteOutboxRepository.findById(outbox.getId()).orElseThrow();
        assertThat(processedOutbox.getStatus()).isEqualTo(ObjectStorageDeleteStatus.DONE);
        assertThat(processedOutbox.getLastError()).isNull();
        assertThat(processedOutbox.getLastAttemptedAt()).isNotNull();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestObjectStorageConfig {

        @Bean
        @Primary
        ObjectStorageClient objectStorageClient() {
            return mock(ObjectStorageClient.class);
        }
    }
}
