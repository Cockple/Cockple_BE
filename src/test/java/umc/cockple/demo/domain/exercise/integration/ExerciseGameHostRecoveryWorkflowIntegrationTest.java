package umc.cockple.demo.domain.exercise.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import umc.cockple.demo.domain.chat.service.ChatMemberAnonymizationService;
import umc.cockple.demo.domain.chat.service.command.PartyChatRoomLifecycleService;
import umc.cockple.demo.domain.chat.service.websocket.send.ChatSendService;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.exercise.service.command.ExerciseGameHostRecoveryService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.enums.MemberStatus;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.member.service.MemberCommandService;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.domain.PartyAddr;
import umc.cockple.demo.domain.party.repository.PartyAddrRepository;
import umc.cockple.demo.domain.party.repository.PartyRepository;
import umc.cockple.demo.domain.party.service.PartyCommandService;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.global.enums.Role;
import umc.cockple.demo.global.oauth2.service.KakaoOauthService;
import umc.cockple.demo.support.IntegrationTestBase;
import umc.cockple.demo.support.fixture.ExerciseFixture;
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

class ExerciseGameHostRecoveryWorkflowIntegrationTest extends IntegrationTestBase {

    @Autowired PartyCommandService partyCommandService;
    @Autowired MemberCommandService memberCommandService;
    @Autowired ExerciseRepository exerciseRepository;
    @Autowired MemberPartyRepository memberPartyRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired PartyRepository partyRepository;
    @Autowired PartyAddrRepository partyAddrRepository;
    @Autowired StringRedisTemplate stringRedisTemplate;

    @MockitoSpyBean ExerciseGameHostRecoveryService exerciseGameHostRecoveryService;
    @MockitoBean PartyChatRoomLifecycleService partyChatRoomLifecycleService;
    @MockitoBean ChatSendService chatSendService;
    @MockitoBean ChatMemberAnonymizationService chatMemberAnonymizationService;
    @MockitoBean KakaoOauthService kakaoOauthService;

    private Member manager;
    private Member targetMember;
    private Party party;
    private Exercise exercise;

    @BeforeEach
    void setUp() {
        manager = memberRepository.save(
                MemberFixture.createMember("모임장", Gender.MALE, Level.A, 5001L));
        targetMember = memberRepository.save(
                MemberFixture.createMember("게임 진행자", Gender.FEMALE, Level.B, 5002L));

        PartyAddr partyAddr = partyAddrRepository.save(
                PartyFixture.createPartyAddr("서울특별시", "영등포구"));
        party = partyRepository.save(
                PartyFixture.createParty("진행자 복구 흐름 테스트 모임", manager.getId(), partyAddr));

        memberPartyRepository.save(
                MemberFixture.createMemberParty(party, manager, Role.PARTY_MANAGER));
        memberPartyRepository.save(
                MemberFixture.createMemberParty(party, targetMember, Role.PARTY_MEMBER));

        exercise = ExerciseFixture.createExerciseWithAddr(party, LocalDate.of(2099, 12, 31));
        ReflectionTestUtils.setField(exercise, "gameHostId", targetMember.getId());
        exercise = exerciseRepository.save(exercise);
    }

    @AfterEach
    void tearDown() {
        exerciseRepository.deleteAll();
        memberPartyRepository.deleteAll();
        partyRepository.deleteAll();
        partyAddrRepository.deleteAll();
        memberRepository.deleteAll();
        stringRedisTemplate.delete("member:tokenVersion:" + targetMember.getId());
    }

    @Test
    @DisplayName("실제 모임 탈퇴가 반환되기 전에 게임 진행자를 모임장으로 복구한다")
    void leavePartyRecoversGameHostSynchronously() {
        partyCommandService.leaveParty(party.getId(), targetMember.getId());

        assertMembershipRemoved();
        assertGameHostRestoredToManager();
        verify(exerciseGameHostRecoveryService)
                .recoverAfterPartyMemberLeft(party.getId(), targetMember.getId());
        verify(partyChatRoomLifecycleService)
                .leavePartyChatRoom(party.getId(), targetMember.getId());
    }

    @Test
    @DisplayName("실제 모임 강퇴가 반환되기 전에 게임 진행자를 모임장으로 복구한다")
    void removeMemberRecoversGameHostSynchronously() {
        partyCommandService.removeMember(
                party.getId(), targetMember.getId(), manager.getId());

        assertMembershipRemoved();
        assertGameHostRestoredToManager();
        verify(exerciseGameHostRecoveryService)
                .recoverAfterPartyMemberLeft(party.getId(), targetMember.getId());
        verify(partyChatRoomLifecycleService)
                .leavePartyChatRoom(party.getId(), targetMember.getId());
    }

    @Test
    @DisplayName("실제 회원탈퇴가 반환되기 전에 모든 진행자를 모임장으로 복구한다")
    void withdrawMemberRecoversGameHostSynchronously() {
        memberCommandService.withdrawMember(targetMember.getId());

        assertGameHostRestoredToManager();
        assertThat(memberRepository.findById(targetMember.getId()).orElseThrow().getIsActive())
                .isEqualTo(MemberStatus.INACTIVE);
        verify(exerciseGameHostRecoveryService)
                .recoverAfterMemberWithdrawn(targetMember.getId());
        verify(chatMemberAnonymizationService)
                .anonymizeDirectDisplayNames(targetMember.getId());
    }

    @Test
    @DisplayName("진행자 복구가 실패하면 모임 탈퇴와 membership 삭제도 롤백한다")
    void recoveryFailureRollsBackPartyLeave() {
        doThrow(new IllegalStateException("진행자 복구 실패"))
                .when(exerciseGameHostRecoveryService)
                .recoverAfterPartyMemberLeft(party.getId(), targetMember.getId());

        assertThatThrownBy(() -> partyCommandService.leaveParty(
                party.getId(), targetMember.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("진행자 복구 실패");

        assertThat(memberPartyRepository.existsByPartyIdAndMemberId(
                party.getId(), targetMember.getId())).isTrue();
        assertThat(exerciseRepository.findById(exercise.getId()).orElseThrow().getGameHostId())
                .isEqualTo(targetMember.getId());
    }

    private void assertMembershipRemoved() {
        assertThat(memberPartyRepository.existsByPartyIdAndMemberId(
                party.getId(), targetMember.getId())).isFalse();
    }

    private void assertGameHostRestoredToManager() {
        assertThat(exerciseRepository.findById(exercise.getId()).orElseThrow().getGameHostId())
                .isEqualTo(manager.getId());
    }
}
