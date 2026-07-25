package umc.cockple.demo.domain.exercise.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;
import umc.cockple.demo.domain.bookmark.repository.ExerciseBookmarkRepository;
import umc.cockple.demo.domain.exercise.converter.query.PartyExerciseCalendarQueryMapper;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.domain.Guest;
import umc.cockple.demo.domain.exercise.dto.map.ExerciseBuildingDetailDTO;
import umc.cockple.demo.domain.exercise.dto.lifecycle.ExerciseDetailDTO;
import umc.cockple.demo.domain.exercise.dto.lifecycle.ExerciseEditDetailDTO;
import umc.cockple.demo.domain.exercise.dto.map.ExerciseMapBuildingsDTO;
import umc.cockple.demo.domain.exercise.dto.guest.ExerciseMyGuestListDTO;
import umc.cockple.demo.domain.exercise.dto.my.MyExerciseCalendarDTO;
import umc.cockple.demo.domain.exercise.dto.my.MyExerciseListDTO;
import umc.cockple.demo.domain.exercise.dto.my.MyPartyExerciseCalendarDTO;
import umc.cockple.demo.domain.exercise.dto.my.MyPartyExerciseDTO;
import umc.cockple.demo.domain.exercise.dto.party.PartyExerciseCalendarDTO;
import umc.cockple.demo.domain.exercise.enums.MyExerciseFilterType;
import umc.cockple.demo.domain.exercise.enums.MyExerciseOrderType;
import umc.cockple.demo.domain.exercise.enums.MyPartyExerciseOrderType;
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.exception.ExerciseException;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.exercise.repository.GuestRepository;
import umc.cockple.demo.domain.exercise.service.query.lookup.ExerciseParticipantCountLookupService;
import umc.cockple.demo.domain.bookmark.service.query.lookup.ExerciseBookmarkLookupService;
import umc.cockple.demo.domain.exercise.service.support.reader.MemberExerciseReader;
import umc.cockple.demo.domain.exercise.service.support.reader.ExerciseReader;
import umc.cockple.demo.domain.exercise.service.support.reader.GuestReader;
import umc.cockple.demo.domain.exercise.service.query.PartyExerciseQueryService;
import umc.cockple.demo.domain.member.service.query.lookup.MemberLookupService;
import umc.cockple.demo.domain.party.service.query.lookup.PartyLookupService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.exercise.domain.MemberExercise;
import umc.cockple.demo.domain.member.domain.MemberParty;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.exception.MemberException;
import umc.cockple.demo.domain.exercise.repository.MemberExerciseRepository;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.member.service.query.lookup.MemberPartyLookupService;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.enums.PartyStatus;
import umc.cockple.demo.domain.party.exception.PartyErrorCode;
import umc.cockple.demo.domain.party.exception.PartyException;
import umc.cockple.demo.domain.party.repository.PartyRepository;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.global.enums.Role;
import umc.cockple.demo.support.ExerciseCalendarTestHelper;
import umc.cockple.demo.support.fixture.ExerciseFixture;
import umc.cockple.demo.support.fixture.GuestFixture;
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("PartyExerciseQueryService")
class PartyExerciseQueryServiceTest {

    private PartyExerciseQueryService partyExerciseQueryService;

    @Mock private ExerciseRepository exerciseRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private MemberPartyRepository memberPartyRepository;
    @Mock private MemberExerciseRepository memberExerciseRepository;
    @Mock private PartyRepository partyRepository;
    @Mock private ExerciseBookmarkRepository exerciseBookmarkRepository;

    private Party party;
    private Exercise exercise;

    @BeforeEach
    void setUp() {
        PartyExerciseCalendarQueryMapper partyExerciseCalendarMapper = new PartyExerciseCalendarQueryMapper();
        partyExerciseQueryService = new PartyExerciseQueryService(
                new ExerciseReader(exerciseRepository),
                new MemberExerciseReader(memberExerciseRepository),
                new ExerciseParticipantCountLookupService(exerciseRepository),
                new ExerciseBookmarkLookupService(exerciseBookmarkRepository),
                new MemberLookupService(memberRepository),
                new PartyLookupService(partyRepository),
                new MemberPartyLookupService(memberPartyRepository),
                partyExerciseCalendarMapper
        );

        Member manager = MemberFixture.createMember("모임장", Gender.MALE, Level.A, 1001L);
        ReflectionTestUtils.setField(manager, "id", 1L);

        party = PartyFixture.createParty("테스트 모임", manager.getId(),
                PartyFixture.createPartyAddr("서울특별시", "강남구"));
        ReflectionTestUtils.setField(party, "id", 10L);

        exercise = ExerciseFixture.createExercise(party, LocalDate.now().minusDays(1));
        ReflectionTestUtils.setField(exercise, "id", 100L);
        ReflectionTestUtils.setField(exercise, "exerciseAddr", ExerciseFixture.createExerciseAddr());
    }

    @Nested
    @DisplayName("getPartyExerciseCalendar")
    class GetPartyExerciseCalendar {

        private Member partyMember;
        private Member outsiderMember;
        private LocalDate startDate;
        private LocalDate endDate;

        @BeforeEach
        void setUp() {
            partyMember = MemberFixture.createMember("파티멤버", Gender.FEMALE, Level.B, 3001L);
            ReflectionTestUtils.setField(partyMember, "id", 2L);

            outsiderMember = MemberFixture.createMember("외부멤버", Gender.MALE, Level.C, 3002L);
            ReflectionTestUtils.setField(outsiderMember, "id", 3L);

            party.addLevel(Gender.FEMALE, Level.B);
            party.addLevel(Gender.MALE, Level.A);

            startDate = LocalDate.of(2026, 3, 23);
            endDate = LocalDate.of(2026, 3, 29);

            ReflectionTestUtils.setField(exercise, "date", LocalDate.of(2026, 3, 24));
        }

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("모임 운동 캘린더를 주차별_일자별로 반환한다")
            void 모임_운동_캘린더를_주차별_일자별로_반환한다() {
                // given
                given(partyRepository.findByIdWithLevels(party.getId()))
                        .willReturn(Optional.of(party));
                given(memberRepository.findById(partyMember.getId()))
                        .willReturn(Optional.of(partyMember));
                given(memberPartyRepository.existsByPartyAndMember(party, partyMember))
                        .willReturn(true);
                given(exerciseRepository.findByPartyIdAndDateRange(party.getId(), startDate, endDate))
                        .willReturn(List.of(exercise));
                given(exerciseRepository.findExerciseParticipantCounts(party.getId(), startDate, endDate))
                        .willReturn(java.util.Collections.singletonList(new Object[]{exercise.getId(), 2}));
                given(exerciseBookmarkRepository.findAllExerciseIdsByMemberIdAndExerciseIds(
                        partyMember.getId(), List.of(exercise.getId())))
                        .willReturn(List.of(exercise.getId()));
                given(memberExerciseRepository.findAllExerciseIdsByMemberAndExerciseIds(
                        partyMember.getId(), List.of(exercise.getId())))
                        .willReturn(List.of(exercise.getId()));

                // when
                PartyExerciseCalendarDTO.Response response = partyExerciseQueryService.getPartyExerciseCalendar(
                        party.getId(), partyMember.getId(), startDate, endDate);

                // then
                assertThat(response.startDate()).isEqualTo(startDate);
                assertThat(response.endDate()).isEqualTo(endDate);
                assertThat(response.isMember()).isTrue();
                assertThat(response.partyName()).isEqualTo(party.getPartyName());
                assertThat(response.weeks()).hasSize(1);
                assertThat(response.weeks().get(0).weekStartDate()).isEqualTo(startDate);
                assertThat(response.weeks().get(0).weekEndDate()).isEqualTo(endDate);
                assertThat(response.weeks().get(0).days()).hasSize(7);
                assertThat(response.weeks().get(0).days().get(1).date())
                        .isEqualTo(LocalDate.of(2026, 3, 24));
                assertThat(response.weeks().get(0).days().get(1).exercises())
                        .extracting(
                                PartyExerciseCalendarDTO.ExerciseCalendarItem::exerciseId,
                                PartyExerciseCalendarDTO.ExerciseCalendarItem::isBookmarked,
                                PartyExerciseCalendarDTO.ExerciseCalendarItem::buildingName,
                                PartyExerciseCalendarDTO.ExerciseCalendarItem::currentParticipants,
                                PartyExerciseCalendarDTO.ExerciseCalendarItem::maxCapacity,
                                PartyExerciseCalendarDTO.ExerciseCalendarItem::isParticipating)
                        .containsExactly(tuple(exercise.getId(), true, "테스트 체육관", 2, 10, true));
            }

            @Test
            @DisplayName("기간 내 운동이 없으면 빈 캘린더를 반환한다")
            void 기간_내_운동이_없으면_빈_캘린더를_반환한다() {
                // given
                given(partyRepository.findByIdWithLevels(party.getId()))
                        .willReturn(Optional.of(party));
                given(memberRepository.findById(outsiderMember.getId()))
                        .willReturn(Optional.of(outsiderMember));
                given(memberPartyRepository.existsByPartyAndMember(party, outsiderMember))
                        .willReturn(false);
                given(exerciseRepository.findByPartyIdAndDateRange(party.getId(), startDate, endDate))
                        .willReturn(List.of());

                // when
                PartyExerciseCalendarDTO.Response response = partyExerciseQueryService.getPartyExerciseCalendar(
                        party.getId(), outsiderMember.getId(), startDate, endDate);

                // then
                assertThat(response.startDate()).isEqualTo(startDate);
                assertThat(response.endDate()).isEqualTo(endDate);
                assertThat(response.isMember()).isFalse();
                assertThat(response.partyName()).isEqualTo(party.getPartyName());
                assertThat(response.weeks()).isEmpty();
            }

            @Test
            @DisplayName("시작일과_종료일이_없으면_기본_기간이_적용된다")
            void 시작일과_종료일이_없으면_기본_기간이_적용된다() {
                // given
                LocalDate expectedStart = ExerciseCalendarTestHelper.expectedDefaultStartDate();
                LocalDate expectedEnd = ExerciseCalendarTestHelper.expectedDefaultEndDate();

                given(partyRepository.findByIdWithLevels(party.getId()))
                        .willReturn(Optional.of(party));
                given(memberRepository.findById(partyMember.getId()))
                        .willReturn(Optional.of(partyMember));
                given(memberPartyRepository.existsByPartyAndMember(party, partyMember))
                        .willReturn(true);
                given(exerciseRepository.findByPartyIdAndDateRange(party.getId(), expectedStart, expectedEnd))
                        .willReturn(List.of());

                // when
                PartyExerciseCalendarDTO.Response response = partyExerciseQueryService.getPartyExerciseCalendar(
                        party.getId(), partyMember.getId(), null, null);

                // then
                assertThat(response.startDate()).isEqualTo(expectedStart);
                assertThat(response.endDate()).isEqualTo(expectedEnd);
                assertThat(response.isMember()).isTrue();
                assertThat(response.weeks()).isEmpty();
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("존재하지 않는 모임이면 PartyException(PARTY_NOT_FOUND)을 던진다")
            void 존재하지_않는_모임이면_예외를_던진다() {
                // given
                given(partyRepository.findByIdWithLevels(999L))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> partyExerciseQueryService.getPartyExerciseCalendar(
                        999L, partyMember.getId(), startDate, endDate))
                        .isInstanceOf(PartyException.class)
                        .hasFieldOrPropertyWithValue("code", PartyErrorCode.PARTY_NOT_FOUND);
                verify(memberRepository, never()).findById(any());
            }

            @Test
            @DisplayName("존재하지 않는 멤버면 MemberException(MEMBER_NOT_FOUND)을 던진다")
            void 존재하지_않는_멤버면_예외를_던진다() {
                // given
                given(partyRepository.findByIdWithLevels(party.getId()))
                        .willReturn(Optional.of(party));
                given(memberRepository.findById(999L))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> partyExerciseQueryService.getPartyExerciseCalendar(
                        party.getId(), 999L, startDate, endDate))
                        .isInstanceOf(MemberException.class)
                        .hasFieldOrPropertyWithValue("code", MemberErrorCode.MEMBER_NOT_FOUND);
            }

            @Test
            @DisplayName("시작일과 종료일이 함께 오지 않으면 예외를 던진다")
            void 시작일과_종료일이_함께_오지_않으면_예외를_던진다() {
                // given
                given(partyRepository.findByIdWithLevels(party.getId()))
                        .willReturn(Optional.of(party));
                given(memberRepository.findById(partyMember.getId()))
                        .willReturn(Optional.of(partyMember));

                // when & then
                assertThatThrownBy(() -> partyExerciseQueryService.getPartyExerciseCalendar(
                        party.getId(), partyMember.getId(), startDate, null))
                        .isInstanceOf(ExerciseException.class)
                        .hasFieldOrPropertyWithValue("code", ExerciseErrorCode.INCOMPLETE_DATE_RANGE);
            }

            @Test
            @DisplayName("삭제된 모임이면 예외를 던진다")
            void 삭제된_모임이면_예외를_던진다() {
                // given
                ReflectionTestUtils.setField(party, "status", PartyStatus.INACTIVE);

                given(partyRepository.findByIdWithLevels(party.getId()))
                        .willReturn(Optional.of(party));
                given(memberRepository.findById(partyMember.getId()))
                        .willReturn(Optional.of(partyMember));

                // when & then
                assertThatThrownBy(() -> partyExerciseQueryService.getPartyExerciseCalendar(
                        party.getId(), partyMember.getId(), startDate, endDate))
                        .isInstanceOf(PartyException.class)
                        .hasFieldOrPropertyWithValue("code", PartyErrorCode.PARTY_IS_DELETED);
            }
        }
    }

}
