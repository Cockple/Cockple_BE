package umc.cockple.demo.domain.party.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import umc.cockple.demo.domain.chat.service.ChatRoomService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberParty;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.domain.PartyAddr;
import umc.cockple.demo.domain.party.events.PartyMemberJoinedEvent;
import umc.cockple.demo.domain.party.exception.PartyErrorCode;
import umc.cockple.demo.domain.party.exception.PartyException;
import umc.cockple.demo.domain.party.repository.PartyRepository;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.global.enums.Role;
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PartyCommandServiceTest {

    @InjectMocks
    private PartyCommandServiceImpl partyCommandService;

    @Mock
    private PartyRepository partyRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private MemberPartyRepository memberPartyRepository;
    @Mock
    private ChatRoomService chatRoomService;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Nested
    @DisplayName("leaveParty")
    class LeaveParty {

        @Test
        @DisplayName("성공 - 일반 멤버가 모임을 탈퇴한다")
        void success_leaveParty() {
            // given
            Long partyId = 1L;
            Long memberId = 10L;

            PartyAddr addr = PartyFixture.createPartyAddr("서울", "강남");
            Member owner = MemberFixture.createMember("모임장", Gender.MALE, Level.A, 1L);
            ReflectionTestUtils.setField(owner, "id", 1L);
            Party party = PartyFixture.createParty("탈퇴 테스트 모임", owner.getId(), addr);
            ReflectionTestUtils.setField(party, "id", partyId);

            Member member = MemberFixture.createMember("일반멤버", Gender.MALE, Level.A, 10L);
            ReflectionTestUtils.setField(member, "id", memberId);

            MemberParty memberParty = MemberFixture.createMemberParty(party, member, Role.party_MEMBER);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
            given(memberPartyRepository.findByPartyAndMember(party, member)).willReturn(Optional.of(memberParty));

            // when
            partyCommandService.leaveParty(partyId, memberId);

            // then
            verify(memberPartyRepository).delete(memberParty);
            verify(chatRoomService).leavePartyChatRoom(partyId, memberId);
            verify(applicationEventPublisher).publishEvent(any(PartyMemberJoinedEvent.class));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 모임인 경우 PARTY_NOT_FOUND 예외가 발생한다")
        void fail_leaveParty_partyNotFound() {
            // given
            given(partyRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> partyCommandService.leaveParty(999L, 1L))
                    .isInstanceOf(PartyException.class)
                    .satisfies(
                            e -> assertThat(((PartyException) e).getCode()).isEqualTo(PartyErrorCode.PARTY_NOT_FOUND));
        }

        @Test
        @DisplayName("실패 - 삭제된 모임인 경우 PARTY_IS_DELETED 예외가 발생한다")
        void fail_leaveParty_partyDeleted() {
            // given
            Long partyId = 1L;
            PartyAddr addr = PartyFixture.createPartyAddr("서울", "강남");
            Party party = PartyFixture.createParty("삭제된 모임", 1L, addr);
            party.delete();

            Member member = MemberFixture.createMember("일반멤버", Gender.MALE, Level.A, 1L);
            ReflectionTestUtils.setField(member, "id", 1L);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(memberRepository.findById(1L)).willReturn(Optional.of(member));

            // when & then
            assertThatThrownBy(() -> partyCommandService.leaveParty(partyId, 1L))
                    .isInstanceOf(PartyException.class)
                    .satisfies(
                            e -> assertThat(((PartyException) e).getCode()).isEqualTo(PartyErrorCode.PARTY_IS_DELETED));
        }

        @Test
        @DisplayName("실패 - 모임장이 탈퇴하려 할 경우 INVALID_ACTION_FOR_OWNER 예외가 발생한다")
        void fail_leaveParty_isOwner() {
            // given
            Long partyId = 1L;
            Long ownerId = 1L;

            PartyAddr addr = PartyFixture.createPartyAddr("서울", "강남");
            Member owner = MemberFixture.createMember("모임장", Gender.MALE, Level.A, 1L);
            ReflectionTestUtils.setField(owner, "id", ownerId);
            Party party = PartyFixture.createParty("탈퇴 테스트 모임", owner.getId(), addr);
            ReflectionTestUtils.setField(party, "id", partyId);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(memberRepository.findById(ownerId)).willReturn(Optional.of(owner));

            // when & then
            assertThatThrownBy(() -> partyCommandService.leaveParty(partyId, ownerId))
                    .isInstanceOf(PartyException.class)
                    .satisfies(e -> assertThat(((PartyException) e).getCode())
                            .isEqualTo(PartyErrorCode.INVALID_ACTION_FOR_OWNER));
        }

        @Test
        @DisplayName("실패 - 부모임장이 탈퇴하려 할 경우 INVALID_ACTION_FOR_SUBOWNER 예외가 발생한다")
        void fail_leaveParty_isSubOwner() {
            // given
            Long partyId = 1L;
            Long subManagerId = 2L;

            PartyAddr addr = PartyFixture.createPartyAddr("서울", "강남");
            Member owner = MemberFixture.createMember("모임장", Gender.MALE, Level.A, 1L);
            ReflectionTestUtils.setField(owner, "id", 1L);
            Party party = PartyFixture.createParty("탈퇴 테스트 모임", owner.getId(), addr);
            ReflectionTestUtils.setField(party, "id", partyId);

            Member subManager = MemberFixture.createMember("부모임장", Gender.MALE, Level.A, 2L);
            ReflectionTestUtils.setField(subManager, "id", subManagerId);

            MemberParty subManagerParty = MemberFixture.createMemberParty(party, subManager, Role.party_SUBMANAGER);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(memberRepository.findById(subManagerId)).willReturn(Optional.of(subManager));
            given(memberPartyRepository.findByPartyIdAndRole(partyId, Role.party_SUBMANAGER))
                    .willReturn(Optional.of(subManagerParty));

            // when & then
            assertThatThrownBy(() -> partyCommandService.leaveParty(partyId, subManagerId))
                    .isInstanceOf(PartyException.class)
                    .satisfies(e -> assertThat(((PartyException) e).getCode())
                            .isEqualTo(PartyErrorCode.INVALID_ACTION_FOR_SUBOWNER));
        }

        @Test
        @DisplayName("실패 - 모임 멤버가 아닌 경우 NOT_MEMBER 예외가 발생한다")
        void fail_leaveParty_notMember() {
            // given
            Long partyId = 1L;
            Long memberId = 10L;

            PartyAddr addr = PartyFixture.createPartyAddr("서울", "강남");
            Party party = PartyFixture.createParty("탈퇴 테스트 모임", 1L, addr);
            ReflectionTestUtils.setField(party, "id", partyId);
            Member member = MemberFixture.createMember("외부인", Gender.MALE, Level.A, 10L);
            ReflectionTestUtils.setField(member, "id", memberId);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
            given(memberPartyRepository.findByPartyAndMember(party, member)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> partyCommandService.leaveParty(partyId, memberId))
                    .isInstanceOf(PartyException.class)
                    .satisfies(e -> assertThat(((PartyException) e).getCode()).isEqualTo(PartyErrorCode.NOT_MEMBER));
        }
    }
}
