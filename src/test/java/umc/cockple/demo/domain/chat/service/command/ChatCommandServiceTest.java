package umc.cockple.demo.domain.chat.service.command;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import umc.cockple.demo.domain.chat.converter.ChatConverter;
import umc.cockple.demo.domain.chat.domain.ChatRoom;
import umc.cockple.demo.domain.chat.domain.ChatRoomMember;
import umc.cockple.demo.domain.chat.dto.DirectChatRoomCreateDTO;
import umc.cockple.demo.domain.chat.enums.ChatRoomMemberStatus;
import umc.cockple.demo.domain.chat.enums.ChatRoomType;
import umc.cockple.demo.domain.chat.exception.ChatErrorCode;
import umc.cockple.demo.domain.chat.exception.ChatException;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomRepository;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.exception.MemberException;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.fixture.ChatFixture;
import umc.cockple.demo.support.fixture.MemberFixture;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatCommandService")
class ChatCommandServiceTest {

    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatRoomMemberRepository chatRoomMemberRepository;
    @Mock private MemberRepository memberRepository;

    private ChatCommandServiceImpl chatCommandService;
    private Member member;
    private Member targetMember;

    @BeforeEach
    void setUp() {
        chatCommandService = new ChatCommandServiceImpl(
                chatRoomRepository,
                chatRoomMemberRepository,
                memberRepository,
                new ChatConverter()
        );

        member = MemberFixture.createMemberWithName("홍길동", "길동", Gender.MALE, Level.A, 1001L);
        ReflectionTestUtils.setField(member, "id", 1L);

        targetMember = MemberFixture.createMemberWithName("김철수", "철수", Gender.MALE, Level.B, 2002L);
        ReflectionTestUtils.setField(targetMember, "id", 2L);
    }

    @Nested
    @DisplayName("createDirectChatRoom - 개인 채팅방 생성 및 참여")
    class CreateDirectChatRoom {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("기존 채팅방이 없으면 새로운 개인 채팅방과 JOINED/PENDING 멤버를 생성한다")
            @SuppressWarnings("unchecked")
            void createNewDirectChatRoom() {
                LocalDateTime createdAt = LocalDateTime.of(2026, 4, 3, 12, 0);

                given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));
                given(memberRepository.findById(targetMember.getId())).willReturn(Optional.of(targetMember));
                given(chatRoomRepository.findDirectChatRoomByMemberIds(member.getId(), targetMember.getId()))
                        .willReturn(Optional.empty());
                given(chatRoomRepository.save(any(ChatRoom.class))).willAnswer(invocation -> {
                    ChatRoom savedRoom = invocation.getArgument(0);
                    ReflectionTestUtils.setField(savedRoom, "id", 100L);
                    ReflectionTestUtils.setField(savedRoom, "createdAt", createdAt);
                    return savedRoom;
                });

                DirectChatRoomCreateDTO.Response result =
                        chatCommandService.createDirectChatRoom(member.getId(), targetMember.getId());

                assertThat(result.chatRoomId()).isEqualTo(100L);
                assertThat(result.displayName()).isEqualTo(targetMember.getMemberName());
                assertThat(result.createdAt()).isEqualTo(createdAt);
                assertThat(result.members()).hasSize(2);
                assertThat(result.members())
                        .extracting(DirectChatRoomCreateDTO.MemberInfo::memberId)
                        .containsExactlyInAnyOrder(member.getId(), targetMember.getId());
                assertThat(result.members())
                        .extracting(DirectChatRoomCreateDTO.MemberInfo::memberName)
                        .containsExactlyInAnyOrder(member.getMemberName(), targetMember.getMemberName());

                ArgumentCaptor<ChatRoom> roomCaptor = ArgumentCaptor.forClass(ChatRoom.class);
                then(chatRoomRepository).should().save(roomCaptor.capture());
                ArgumentCaptor<List> membersCaptor = ArgumentCaptor.forClass(List.class);
                then(chatRoomMemberRepository).should().saveAll(membersCaptor.capture());

                ChatRoom savedRoom = roomCaptor.getValue();
                List<ChatRoomMember> savedMembers = membersCaptor.getValue();
                ChatRoomMember myMembership = savedMembers.stream()
                        .filter(chatRoomMember -> chatRoomMember.getMember() == member)
                        .findFirst()
                        .orElseThrow();
                ChatRoomMember targetMembership = savedMembers.stream()
                        .filter(chatRoomMember -> chatRoomMember.getMember() == targetMember)
                        .findFirst()
                        .orElseThrow();

                assertThat(savedRoom.getType()).isEqualTo(ChatRoomType.DIRECT);
                assertThat(savedMembers).hasSize(2);
                assertThat(myMembership.getStatus()).isEqualTo(ChatRoomMemberStatus.JOINED);
                assertThat(myMembership.getDisplayName()).isEqualTo(targetMember.getMemberName());
                assertThat(targetMembership.getStatus()).isEqualTo(ChatRoomMemberStatus.PENDING);
                assertThat(targetMembership.getDisplayName()).isEqualTo(member.getMemberName());
            }

            @Test
            @DisplayName("기존 개인 채팅방이 있으면 새로운 채팅방을 만들지 않고 기존 채팅방을 반환한다")
            void returnExistingDirectChatRoom() {
                ChatRoom existingRoom = ChatFixture.createDirectChatRoom();
                ReflectionTestUtils.setField(existingRoom, "id", 200L);
                ReflectionTestUtils.setField(existingRoom, "createdAt", LocalDateTime.of(2026, 4, 2, 9, 30));

                ChatRoomMember myMembership =
                        ChatRoomMember.createJoined(existingRoom, member, targetMember.getMemberName());
                ChatRoomMember targetMembership =
                        ChatRoomMember.createPending(existingRoom, targetMember, member.getMemberName());

                given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));
                given(memberRepository.findById(targetMember.getId())).willReturn(Optional.of(targetMember));
                given(chatRoomRepository.findDirectChatRoomByMemberIds(member.getId(), targetMember.getId()))
                        .willReturn(Optional.of(existingRoom));
                given(chatRoomMemberRepository.findByChatRoomId(existingRoom.getId()))
                        .willReturn(List.of(myMembership, targetMembership));

                DirectChatRoomCreateDTO.Response result =
                        chatCommandService.createDirectChatRoom(member.getId(), targetMember.getId());

                assertThat(result.chatRoomId()).isEqualTo(existingRoom.getId());
                assertThat(result.displayName()).isEqualTo(targetMember.getMemberName());
                assertThat(result.createdAt()).isEqualTo(existingRoom.getCreatedAt());
                assertThat(result.members()).hasSize(2);
                assertThat(result.members())
                        .extracting(DirectChatRoomCreateDTO.MemberInfo::memberId)
                        .containsExactlyInAnyOrder(member.getId(), targetMember.getId());
                assertThat(result.members())
                        .extracting(DirectChatRoomCreateDTO.MemberInfo::memberName)
                        .containsExactlyInAnyOrder(member.getMemberName(), targetMember.getMemberName());

                then(chatRoomMemberRepository).should().findByChatRoomId(existingRoom.getId());
                then(chatRoomRepository).should(never()).save(any(ChatRoom.class));
                then(chatRoomMemberRepository).should(never()).saveAll(any());
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("자기 자신에게는 개인 채팅방을 생성할 수 없다")
            void cannotChatWithSelf() {
                assertThatThrownBy(() -> chatCommandService.createDirectChatRoom(member.getId(), member.getId()))
                        .isInstanceOf(ChatException.class)
                        .satisfies(e -> assertThat(((ChatException) e).getCode())
                                .isEqualTo(ChatErrorCode.CANNOT_CHAT_WITH_SELF));

                verifyNoInteractions(memberRepository, chatRoomRepository, chatRoomMemberRepository);
            }

            @Test
            @DisplayName("요청 회원이 존재하지 않으면 MemberException(MEMBER_NOT_FOUND)을 던진다")
            void requesterNotFound() {
                given(memberRepository.findById(member.getId())).willReturn(Optional.empty());

                assertThatThrownBy(() -> chatCommandService.createDirectChatRoom(member.getId(), targetMember.getId()))
                        .isInstanceOf(MemberException.class)
                        .satisfies(e -> assertThat(((MemberException) e).getCode())
                                .isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND));

                then(memberRepository).should().findById(member.getId());
                then(memberRepository).should(never()).findById(targetMember.getId());
                verifyNoInteractions(chatRoomRepository, chatRoomMemberRepository);
            }

            @Test
            @DisplayName("상대 회원이 존재하지 않으면 MemberException(MEMBER_NOT_FOUND)을 던진다")
            void targetMemberNotFound() {
                given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));
                given(memberRepository.findById(targetMember.getId())).willReturn(Optional.empty());

                assertThatThrownBy(() -> chatCommandService.createDirectChatRoom(member.getId(), targetMember.getId()))
                        .isInstanceOf(MemberException.class)
                        .satisfies(e -> assertThat(((MemberException) e).getCode())
                                .isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND));

                then(memberRepository).should().findById(member.getId());
                then(memberRepository).should().findById(targetMember.getId());
                then(chatRoomRepository).should(never()).findDirectChatRoomByMemberIds(any(), any());
                then(chatRoomRepository).should(never()).save(any(ChatRoom.class));
                verifyNoInteractions(chatRoomMemberRepository);
            }
        }
    }
}
