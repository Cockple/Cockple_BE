package umc.cockple.demo.domain.exercise.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.domain.ExerciseParticipation;
import umc.cockple.demo.domain.exercise.domain.Guest;
import umc.cockple.demo.domain.exercise.enums.ExerciseMemberShipStatus;
import umc.cockple.demo.domain.exercise.events.ExerciseAttendanceChangedEvent;
import umc.cockple.demo.domain.exercise.service.ExerciseWaitingPromotionService.ParticipantKey;
import umc.cockple.demo.domain.exercise.service.ExerciseWaitingPromotionService.PromotionCandidate;
import umc.cockple.demo.domain.exercise.service.support.calculator.ExerciseParticipantPositionCalculator;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.fixture.ExerciseFixture;
import umc.cockple.demo.support.fixture.GuestFixture;
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ExerciseWaitingPromotionServiceTest {

    @Spy private ExerciseParticipantPositionCalculator positionCalculator = new ExerciseParticipantPositionCalculator();
    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private ExerciseWaitingPromotionService service;

    private Exercise exercise;
    private ExerciseParticipation confirmed;
    private Guest firstWaiting;

    @BeforeEach
    void setUp() {
        Member member = MemberFixture.createMember("참여자", Gender.MALE, Level.B, 1001L);
        ReflectionTestUtils.setField(member, "id", 1L);
        Party party = PartyFixture.createParty("모임", member.getId(), PartyFixture.createPartyAddr("서울", "강남"));
        ReflectionTestUtils.setField(party, "id", 10L);
        exercise = ExerciseFixture.createExerciseWithAddr(party, LocalDate.of(2099, 12, 31), 1);
        ReflectionTestUtils.setField(exercise, "id", 100L);
        confirmed = exercise.addParticipation(member, ExerciseMemberShipStatus.PARTY_MEMBER);
        ReflectionTestUtils.setField(confirmed, "createdAt", LocalDate.of(2099, 1, 1).atStartOfDay());

        firstWaiting = GuestFixture.createGuest(exercise, 99L, "첫 대기 게스트", Gender.FEMALE);
        ReflectionTestUtils.setField(firstWaiting, "id", 2L);
        ReflectionTestUtils.setField(firstWaiting, "createdAt", LocalDate.of(2099, 1, 2).atStartOfDay());
        Member laterMember = MemberFixture.createMember("다음 대기자", Gender.MALE, Level.B, 1002L);
        ReflectionTestUtils.setField(laterMember, "id", 2L);
        ExerciseParticipation laterWaiting = exercise.addParticipation(laterMember, ExerciseMemberShipStatus.PARTY_MEMBER);
        ReflectionTestUtils.setField(laterWaiting, "createdAt", LocalDate.of(2099, 1, 3).atStartOfDay());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("취소 전 한 번만 정렬하고 확정 인원이 빠진 경우에만 첫 대기자에게 알린다")
    void cancellationSortsOnlyOnce(boolean cancelWaitingGuest) {
        ParticipantKey cancelled = cancelWaitingGuest
                ? new ParticipantKey(true, firstWaiting.getId())
                : new ParticipantKey(false, confirmed.getMember().getId());
        Optional<PromotionCandidate> candidate = service.findPromotionCandidate(exercise, cancelled);

        if (cancelWaitingGuest) {
            exercise.removeGuest(firstWaiting);
        } else {
            exercise.removeParticipation(confirmed);
        }
        candidate.ifPresent(promoted -> service.notifyPromoted(exercise, promoted));

        verify(positionCalculator, times(1)).sortByJoinedAt(anyList(), any());
        if (cancelWaitingGuest) {
            assertThat(candidate).isEmpty();
            verifyNoInteractions(eventPublisher);
        } else {
            ArgumentCaptor<ExerciseAttendanceChangedEvent> captor = ArgumentCaptor.forClass(ExerciseAttendanceChangedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().recipientMemberIds()).containsExactly(99L);
            assertThat(captor.getValue().guestName()).isEqualTo("첫 대기 게스트");
        }
    }
}
