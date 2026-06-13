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
import umc.cockple.demo.domain.exercise.converter.ExerciseConverter;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.domain.Guest;
import umc.cockple.demo.domain.exercise.dto.ExerciseBuildingDetailDTO;
import umc.cockple.demo.domain.exercise.dto.ExerciseDetailDTO;
import umc.cockple.demo.domain.exercise.dto.ExerciseEditDetailDTO;
import umc.cockple.demo.domain.exercise.dto.ExerciseMapBuildingsDTO;
import umc.cockple.demo.domain.exercise.dto.ExerciseMyGuestListDTO;
import umc.cockple.demo.domain.exercise.dto.MyExerciseCalendarDTO;
import umc.cockple.demo.domain.exercise.dto.MyExerciseListDTO;
import umc.cockple.demo.domain.exercise.dto.MyPartyExerciseCalendarDTO;
import umc.cockple.demo.domain.exercise.dto.MyPartyExerciseDTO;
import umc.cockple.demo.domain.exercise.dto.PartyExerciseCalendarDTO;
import umc.cockple.demo.domain.exercise.enums.MyExerciseFilterType;
import umc.cockple.demo.domain.exercise.enums.MyExerciseOrderType;
import umc.cockple.demo.domain.exercise.enums.MyPartyExerciseOrderType;
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.exception.ExerciseException;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.exercise.repository.GuestRepository;
import umc.cockple.demo.domain.exercise.service.support.ExerciseBookmarkReader;
import umc.cockple.demo.domain.exercise.service.support.ExerciseParticipantReader;
import umc.cockple.demo.domain.exercise.service.support.ExerciseReader;
import umc.cockple.demo.domain.exercise.service.support.GuestReader;
import umc.cockple.demo.domain.exercise.service.query.ExerciseMyQueryService;
import umc.cockple.demo.domain.member.service.support.MemberLookupService;
import umc.cockple.demo.domain.party.service.support.PartyLookupService;
import umc.cockple.demo.domain.file.service.FileService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberExercise;
import umc.cockple.demo.domain.member.domain.MemberParty;
import umc.cockple.demo.domain.member.repository.MemberExerciseRepository;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.member.repository.MemberRepository;
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
@DisplayName("ExerciseMyQueryService")
class ExerciseMyQueryServiceTest {

    private ExerciseMyQueryService exerciseMyQueryService;

    @Mock private ExerciseRepository exerciseRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private MemberPartyRepository memberPartyRepository;
    @Mock private MemberExerciseRepository memberExerciseRepository;
    @Mock private ExerciseBookmarkRepository exerciseBookmarkRepository;
    @Mock private FileService fileService;

    private Party party;

    @BeforeEach
    void setUp() {
        ExerciseConverter exerciseConverter = new ExerciseConverter(fileService);
        exerciseMyQueryService = new ExerciseMyQueryService(
                new ExerciseReader(exerciseRepository),
                new ExerciseParticipantReader(exerciseRepository, memberExerciseRepository, memberPartyRepository),
                new ExerciseBookmarkReader(exerciseBookmarkRepository),
                new MemberLookupService(memberRepository),
                exerciseConverter
        );

        Member manager = MemberFixture.createMember("모임장", Gender.MALE, Level.A, 1001L);
        ReflectionTestUtils.setField(manager, "id", 1L);

        party = PartyFixture.createParty("테스트 모임", manager.getId(),
                PartyFixture.createPartyAddr("서울특별시", "강남구"));
        ReflectionTestUtils.setField(party, "id", 10L);
    }

    @Nested
    @DisplayName("getMyExerciseCalendar")
    class GetMyExerciseCalendar {

        private Member calendarMember;
        private LocalDate startDate;
        private LocalDate endDate;
        private Exercise myExercise;

        @BeforeEach
        void setUp() {
            calendarMember = MemberFixture.createMember("캘린더멤버", Gender.FEMALE, Level.B, 4001L);
            ReflectionTestUtils.setField(calendarMember, "id", 4L);

            startDate = LocalDate.of(2026, 3, 23);
            endDate = LocalDate.of(2026, 3, 29);

            myExercise = ExerciseFixture.createExerciseWithAddr(party, LocalDate.of(2026, 3, 25));
            ReflectionTestUtils.setField(myExercise, "id", 200L);
        }

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("내 운동 캘린더를 주차별_일자별로 반환한다")
            void 내_운동_캘린더를_주차별_일자별로_반환한다() {
                // given
                given(memberRepository.findById(calendarMember.getId()))
                        .willReturn(Optional.of(calendarMember));
                given(exerciseRepository.findByMemberIdAndDateRange(calendarMember.getId(), startDate, endDate))
                        .willReturn(List.of(myExercise));

                // when
                MyExerciseCalendarDTO.Response response = exerciseMyQueryService.getMyExerciseCalendar(
                        calendarMember.getId(), startDate, endDate);

                // then
                assertThat(response.startDate()).isEqualTo(startDate);
                assertThat(response.endDate()).isEqualTo(endDate);
                assertThat(response.weeks()).hasSize(1);
                assertThat(response.weeks().get(0).weekStartDate()).isEqualTo(startDate);
                assertThat(response.weeks().get(0).weekEndDate()).isEqualTo(endDate);
                assertThat(response.weeks().get(0).days()).hasSize(7);
                assertThat(response.weeks().get(0).days().get(2).date())
                        .isEqualTo(LocalDate.of(2026, 3, 25));
                assertThat(response.weeks().get(0).days().get(2).exercises())
                        .extracting(
                                MyExerciseCalendarDTO.ExerciseCalendarItem::exerciseId,
                                MyExerciseCalendarDTO.ExerciseCalendarItem::partyId,
                                MyExerciseCalendarDTO.ExerciseCalendarItem::partyName,
                                MyExerciseCalendarDTO.ExerciseCalendarItem::buildingName,
                                MyExerciseCalendarDTO.ExerciseCalendarItem::startTime,
                                MyExerciseCalendarDTO.ExerciseCalendarItem::endTime,
                                MyExerciseCalendarDTO.ExerciseCalendarItem::profileImageUrl)
                        .containsExactly(tuple(200L, 10L, "테스트 모임", "테스트 체육관", LocalTime.of(10, 0), null, null));
            }

            @Test
            @DisplayName("기간 내 참여 운동이 없으면 빈 캘린더를 반환한다")
            void 기간_내_참여_운동이_없으면_빈_캘린더를_반환한다() {
                // given
                given(memberRepository.findById(calendarMember.getId()))
                        .willReturn(Optional.of(calendarMember));
                given(exerciseRepository.findByMemberIdAndDateRange(calendarMember.getId(), startDate, endDate))
                        .willReturn(List.of());

                // when
                MyExerciseCalendarDTO.Response response = exerciseMyQueryService.getMyExerciseCalendar(
                        calendarMember.getId(), startDate, endDate);

                // then
                assertThat(response.startDate()).isEqualTo(startDate);
                assertThat(response.endDate()).isEqualTo(endDate);
                assertThat(response.weeks()).isEmpty();
            }

            @Test
            @DisplayName("시작일과_종료일이_없으면_기본_기간이_적용된다")
            void 시작일과_종료일이_없으면_기본_기간이_적용된다() {
                // given
                LocalDate expectedStart = ExerciseCalendarTestHelper.expectedDefaultStartDate();
                LocalDate expectedEnd = ExerciseCalendarTestHelper.expectedDefaultEndDate();

                given(memberRepository.findById(calendarMember.getId()))
                        .willReturn(Optional.of(calendarMember));
                given(exerciseRepository.findByMemberIdAndDateRange(calendarMember.getId(), expectedStart, expectedEnd))
                        .willReturn(List.of());

                // when
                MyExerciseCalendarDTO.Response response = exerciseMyQueryService.getMyExerciseCalendar(
                        calendarMember.getId(), null, null);

                // then
                assertThat(response.startDate()).isEqualTo(expectedStart);
                assertThat(response.endDate()).isEqualTo(expectedEnd);
                assertThat(response.weeks()).isEmpty();
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("존재하지_않는_멤버면_예외를_던진다")
            void 존재하지_않는_멤버면_예외를_던진다() {
                // given
                given(memberRepository.findById(999L))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> exerciseMyQueryService.getMyExerciseCalendar(999L, startDate, endDate))
                        .isInstanceOf(ExerciseException.class)
                        .hasFieldOrPropertyWithValue("code", ExerciseErrorCode.MEMBER_NOT_FOUND);
            }

            @Test
            @DisplayName("시작일과 종료일이 함께 오지 않으면 예외를 던진다")
            void 시작일과_종료일이_함께_오지_않으면_예외를_던진다() {
                // given
                given(memberRepository.findById(calendarMember.getId()))
                        .willReturn(Optional.of(calendarMember));

                // when & then
                assertThatThrownBy(() -> exerciseMyQueryService.getMyExerciseCalendar(
                        calendarMember.getId(), startDate, null))
                        .isInstanceOf(ExerciseException.class)
                        .hasFieldOrPropertyWithValue("code", ExerciseErrorCode.INCOMPLETE_DATE_RANGE);
            }

            @Test
            @DisplayName("시작일이 종료일과 같거나 늦으면 예외를 던진다")
            void 시작일이_종료일과_같거나_늦으면_예외를_던진다() {
                // given
                given(memberRepository.findById(calendarMember.getId()))
                        .willReturn(Optional.of(calendarMember));

                // when & then
                assertThatThrownBy(() -> exerciseMyQueryService.getMyExerciseCalendar(
                        calendarMember.getId(), endDate, startDate))
                        .isInstanceOf(ExerciseException.class)
                        .hasFieldOrPropertyWithValue("code", ExerciseErrorCode.INVALID_DATE_RANGE);
            }
        }
    }

    @Nested
    @DisplayName("getMyPartyExercise")
    class GetMyPartyExercise {

        private Member partyMember;
        private Exercise firstUpcomingExercise;
        private Exercise secondUpcomingExercise;

        @BeforeEach
        void setUp() {
            partyMember = MemberFixture.createMember("내모임멤버", Gender.MALE, Level.B, 5001L);
            ReflectionTestUtils.setField(partyMember, "id", 5L);

            firstUpcomingExercise = ExerciseFixture.createExerciseWithAddr(party, LocalDate.of(2026, 4, 1));
            ReflectionTestUtils.setField(firstUpcomingExercise, "id", 301L);

            secondUpcomingExercise = ExerciseFixture.createExerciseWithAddr(party, LocalDate.of(2026, 4, 2));
            ReflectionTestUtils.setField(secondUpcomingExercise, "id", 302L);
        }

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("내 모임의 예정된 운동 목록을 반환한다")
            void 내_모임의_예정된_운동_목록을_반환한다() {
                // given
                given(memberRepository.findById(partyMember.getId()))
                        .willReturn(Optional.of(partyMember));
                given(memberPartyRepository.findPartyIdsByMemberId(partyMember.getId()))
                        .willReturn(List.of(party.getId()));
                given(exerciseRepository.findRecentExercisesByPartyIds(eq(List.of(party.getId())), argThat(
                        (org.springframework.data.domain.Pageable pageable) -> pageable.getPageNumber() == 0 && pageable.getPageSize() == 6)))
                        .willReturn(List.of(firstUpcomingExercise, secondUpcomingExercise));

                // when
                MyPartyExerciseDTO.Response response = exerciseMyQueryService.getMyPartyExercise(partyMember.getId());

                // then
                assertThat(response.totalExercises()).isEqualTo(2);
                assertThat(response.exercises())
                        .extracting(
                                MyPartyExerciseDTO.Exercises::exerciseId,
                                MyPartyExerciseDTO.Exercises::partyId,
                                MyPartyExerciseDTO.Exercises::partyName,
                                MyPartyExerciseDTO.Exercises::buildingName,
                                MyPartyExerciseDTO.Exercises::date,
                                MyPartyExerciseDTO.Exercises::dayOfWeek,
                                MyPartyExerciseDTO.Exercises::startTime,
                                MyPartyExerciseDTO.Exercises::profileImageUrl)
                        .containsExactly(
                                tuple(301L, 10L, "테스트 모임", "테스트 체육관", LocalDate.of(2026, 4, 1), "WEDNESDAY", LocalTime.of(10, 0), null),
                                tuple(302L, 10L, "테스트 모임", "테스트 체육관", LocalDate.of(2026, 4, 2), "THURSDAY", LocalTime.of(10, 0), null)
                        );
            }

            @Test
            @DisplayName("속한 모임이 없으면 빈 응답을 반환한다")
            void 속한_모임이_없으면_빈_응답을_반환한다() {
                // given
                given(memberRepository.findById(partyMember.getId()))
                        .willReturn(Optional.of(partyMember));
                given(memberPartyRepository.findPartyIdsByMemberId(partyMember.getId()))
                        .willReturn(List.of());

                // when
                MyPartyExerciseDTO.Response response = exerciseMyQueryService.getMyPartyExercise(partyMember.getId());

                // then
                assertThat(response.totalExercises()).isZero();
                assertThat(response.exercises()).isEmpty();
                verify(exerciseRepository, never()).findRecentExercisesByPartyIds(any(), any());
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("존재하지_않는_멤버면_예외를_던진다")
            void 존재하지_않는_멤버면_예외를_던진다() {
                // given
                given(memberRepository.findById(999L))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> exerciseMyQueryService.getMyPartyExercise(999L))
                        .isInstanceOf(ExerciseException.class)
                        .hasFieldOrPropertyWithValue("code", ExerciseErrorCode.MEMBER_NOT_FOUND);
            }
        }
    }

    @Nested
    @DisplayName("getMyPartyExerciseCalendar")
    class GetMyPartyExerciseCalendar {

        private Member calendarMember;
        private Exercise calendarExercise;
        private LocalDate startDate;
        private LocalDate endDate;

        @BeforeEach
        void setUp() {
            calendarMember = MemberFixture.createMember("내모임캘린더멤버", Gender.FEMALE, Level.B, 6001L);
            ReflectionTestUtils.setField(calendarMember, "id", 6L);

            startDate = LocalDate.of(2026, 3, 23);
            endDate = LocalDate.of(2026, 3, 29);

            calendarExercise = ExerciseFixture.createExerciseWithAddr(party, LocalDate.of(2026, 3, 25));
            ReflectionTestUtils.setField(calendarExercise, "id", 400L);
        }

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("내 모임 운동 캘린더를 주차별_일자별로 반환한다")
            void 내_모임_운동_캘린더를_주차별_일자별로_반환한다() {
                // given
                given(memberRepository.findById(calendarMember.getId()))
                        .willReturn(Optional.of(calendarMember));
                given(memberPartyRepository.findPartyIdsByMemberId(calendarMember.getId()))
                        .willReturn(List.of(party.getId()));
                given(exerciseRepository.findByPartyIdsAndDateRange(List.of(party.getId()), startDate, endDate))
                        .willReturn(List.of(calendarExercise));
                given(exerciseBookmarkRepository.findAllExerciseIdsByMemberIdAndExerciseIds(
                        calendarMember.getId(), List.of(calendarExercise.getId())))
                        .willReturn(List.of());
                given(exerciseRepository.findExerciseParticipantCountsByExerciseIds(
                        List.of(calendarExercise.getId()), startDate, endDate))
                        .willReturn(Collections.singletonList(new Object[]{calendarExercise.getId(), 3}));

                // when
                MyPartyExerciseCalendarDTO.Response response = exerciseMyQueryService.getMyPartyExerciseCalendar(
                        calendarMember.getId(), MyPartyExerciseOrderType.LATEST, startDate, endDate);

                // then
                assertThat(response.startDate()).isEqualTo(startDate);
                assertThat(response.endDate()).isEqualTo(endDate);
                assertThat(response.weeks()).hasSize(1);
                assertThat(response.weeks().get(0).weekStartDate()).isEqualTo(startDate);
                assertThat(response.weeks().get(0).weekEndDate()).isEqualTo(endDate);
                assertThat(response.weeks().get(0).days()).hasSize(7);
                assertThat(response.weeks().get(0).days().get(2).date())
                        .isEqualTo(LocalDate.of(2026, 3, 25));
                assertThat(response.weeks().get(0).days().get(2).exercises())
                        .extracting(
                                MyPartyExerciseCalendarDTO.ExerciseCalendarItem::exerciseId,
                                MyPartyExerciseCalendarDTO.ExerciseCalendarItem::partyId,
                                MyPartyExerciseCalendarDTO.ExerciseCalendarItem::partyName,
                                MyPartyExerciseCalendarDTO.ExerciseCalendarItem::buildingName,
                                MyPartyExerciseCalendarDTO.ExerciseCalendarItem::isBookmarked,
                                MyPartyExerciseCalendarDTO.ExerciseCalendarItem::nowCapacity)
                        .containsExactly(tuple(400L, 10L, "테스트 모임", "테스트 체육관", false, 3));
            }

            @Test
            @DisplayName("북마크한 운동은 isBookmarked가 true로 반환된다")
            void 북마크한_운동은_isBookmarked가_true로_반환된다() {
                // given
                given(memberRepository.findById(calendarMember.getId()))
                        .willReturn(Optional.of(calendarMember));
                given(memberPartyRepository.findPartyIdsByMemberId(calendarMember.getId()))
                        .willReturn(List.of(party.getId()));
                given(exerciseRepository.findByPartyIdsAndDateRange(List.of(party.getId()), startDate, endDate))
                        .willReturn(List.of(calendarExercise));
                given(exerciseBookmarkRepository.findAllExerciseIdsByMemberIdAndExerciseIds(
                        calendarMember.getId(), List.of(calendarExercise.getId())))
                        .willReturn(List.of(calendarExercise.getId()));
                given(exerciseRepository.findExerciseParticipantCountsByExerciseIds(
                        List.of(calendarExercise.getId()), startDate, endDate))
                        .willReturn(List.of());

                // when
                MyPartyExerciseCalendarDTO.Response response = exerciseMyQueryService.getMyPartyExerciseCalendar(
                        calendarMember.getId(), MyPartyExerciseOrderType.LATEST, startDate, endDate);

                // then
                assertThat(response.weeks().get(0).days().get(2).exercises().get(0).isBookmarked()).isTrue();
            }

            @Test
            @DisplayName("속한 모임이 없으면 빈 캘린더를 반환한다")
            void 속한_모임이_없으면_빈_캘린더를_반환한다() {
                // given
                given(memberRepository.findById(calendarMember.getId()))
                        .willReturn(Optional.of(calendarMember));
                given(memberPartyRepository.findPartyIdsByMemberId(calendarMember.getId()))
                        .willReturn(List.of());

                // when
                MyPartyExerciseCalendarDTO.Response response = exerciseMyQueryService.getMyPartyExerciseCalendar(
                        calendarMember.getId(), MyPartyExerciseOrderType.LATEST, startDate, endDate);

                // then
                assertThat(response.startDate()).isEqualTo(startDate);
                assertThat(response.endDate()).isEqualTo(endDate);
                assertThat(response.weeks()).isEmpty();
                verify(exerciseRepository, never()).findByPartyIdsAndDateRange(any(), any(), any());
            }

            @Test
            @DisplayName("기간 내 내 모임 운동이 없으면 빈 캘린더를 반환한다")
            void 기간_내_내_모임_운동이_없으면_빈_캘린더를_반환한다() {
                // given
                given(memberRepository.findById(calendarMember.getId()))
                        .willReturn(Optional.of(calendarMember));
                given(memberPartyRepository.findPartyIdsByMemberId(calendarMember.getId()))
                        .willReturn(List.of(party.getId()));
                given(exerciseRepository.findByPartyIdsAndDateRange(List.of(party.getId()), startDate, endDate))
                        .willReturn(List.of());

                // when
                MyPartyExerciseCalendarDTO.Response response = exerciseMyQueryService.getMyPartyExerciseCalendar(
                        calendarMember.getId(), MyPartyExerciseOrderType.LATEST, startDate, endDate);

                // then
                assertThat(response.startDate()).isEqualTo(startDate);
                assertThat(response.endDate()).isEqualTo(endDate);
                assertThat(response.weeks()).isEmpty();
            }

            @Test
            @DisplayName("시작일과_종료일이_없으면_기본_기간이_적용된다")
            void 시작일과_종료일이_없으면_기본_기간이_적용된다() {
                // given
                LocalDate expectedStart = ExerciseCalendarTestHelper.expectedDefaultStartDate();
                LocalDate expectedEnd = ExerciseCalendarTestHelper.expectedDefaultEndDate();

                given(memberRepository.findById(calendarMember.getId()))
                        .willReturn(Optional.of(calendarMember));
                given(memberPartyRepository.findPartyIdsByMemberId(calendarMember.getId()))
                        .willReturn(List.of(party.getId()));
                given(exerciseRepository.findByPartyIdsAndDateRange(List.of(party.getId()), expectedStart, expectedEnd))
                        .willReturn(List.of());

                // when
                MyPartyExerciseCalendarDTO.Response response = exerciseMyQueryService.getMyPartyExerciseCalendar(
                        calendarMember.getId(), MyPartyExerciseOrderType.LATEST, null, null);

                // then
                assertThat(response.startDate()).isEqualTo(expectedStart);
                assertThat(response.endDate()).isEqualTo(expectedEnd);
                assertThat(response.weeks()).isEmpty();
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("존재하지_않는_멤버면_예외를_던진다")
            void 존재하지_않는_멤버면_예외를_던진다() {
                // given
                given(memberRepository.findById(999L))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> exerciseMyQueryService.getMyPartyExerciseCalendar(
                        999L, MyPartyExerciseOrderType.LATEST, startDate, endDate))
                        .isInstanceOf(ExerciseException.class)
                        .hasFieldOrPropertyWithValue("code", ExerciseErrorCode.MEMBER_NOT_FOUND);
            }
        }
    }

    @Nested
    @DisplayName("getMyExercises")
    class GetMyExercises {

        private Member myExerciseMember;
        private Exercise completedExercise;
        private Exercise upcomingExercise;
        private Exercise futureLatestExercise;
        private Pageable firstPage;

        @BeforeEach
        void setUp() {
            myExerciseMember = MemberFixture.createMember("내참여운동멤버", Gender.MALE, Level.B, 7001L,
                    LocalDate.of(2000, 1, 1));
            ReflectionTestUtils.setField(myExerciseMember, "id", 7L);

            party.addLevel(Gender.FEMALE, Level.B);
            party.addLevel(Gender.MALE, Level.A);

            completedExercise = createMyExercise(701L, LocalDate.of(2024, 1, 5),
                    LocalTime.of(9, 0), LocalTime.of(11, 0), 18, false);
            upcomingExercise = createMyExercise(702L, LocalDate.of(2099, 1, 3),
                    LocalTime.of(18, 0), null, 12, true);
            futureLatestExercise = createMyExercise(703L, LocalDate.of(2099, 1, 10),
                    LocalTime.of(7, 30), LocalTime.of(9, 0), 20, true);
            firstPage = PageRequest.of(0, 2);
        }

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("ALL 최신순은 전체 운동 리포지토리를 날짜 내림차순으로 호출한다")
            void ALL_최신순은_전체_운동_리포지토리를_날짜_내림차순으로_호출한다() {
                // given
                given(memberRepository.findById(myExerciseMember.getId()))
                        .willReturn(Optional.of(myExerciseMember));
                given(exerciseRepository.findMyExercisesWithPaging(eq(myExerciseMember.getId()), argThat(
                        pageable -> matchesSort(pageable, Sort.Direction.DESC, Sort.Direction.DESC))))
                        .willReturn(emptySlice(firstPage));

                // when
                MyExerciseListDTO.Response response = exerciseMyQueryService.getMyExercises(
                        myExerciseMember.getId(), MyExerciseFilterType.ALL, MyExerciseOrderType.LATEST, firstPage);

                // then
                assertThat(response.totalCount()).isZero();
                assertThat(response.hasNext()).isFalse();
                assertThat(response.exercises()).isEmpty();
                verify(exerciseRepository).findMyExercisesWithPaging(eq(myExerciseMember.getId()), any(Pageable.class));
                verify(exerciseRepository, never()).findMyUpcomingExercisesWithPaging(any(), any());
                verify(exerciseRepository, never()).findMyCompletedExercisesWithPaging(any(), any());
            }

            @Test
            @DisplayName("UPCOMING 최신순은 예정 운동 리포지토리를 날짜 오름차순으로 호출한다")
            void UPCOMING_최신순은_예정_운동_리포지토리를_날짜_오름차순으로_호출한다() {
                // given
                given(memberRepository.findById(myExerciseMember.getId()))
                        .willReturn(Optional.of(myExerciseMember));
                given(exerciseRepository.findMyUpcomingExercisesWithPaging(eq(myExerciseMember.getId()), argThat(
                        pageable -> matchesSort(pageable, Sort.Direction.ASC, Sort.Direction.ASC))))
                        .willReturn(emptySlice(firstPage));

                // when
                exerciseMyQueryService.getMyExercises(
                        myExerciseMember.getId(), MyExerciseFilterType.UPCOMING, MyExerciseOrderType.LATEST, firstPage);

                // then
                verify(exerciseRepository, never()).findMyExercisesWithPaging(any(), any());
                verify(exerciseRepository).findMyUpcomingExercisesWithPaging(eq(myExerciseMember.getId()), any(Pageable.class));
                verify(exerciseRepository, never()).findMyCompletedExercisesWithPaging(any(), any());
            }

            @Test
            @DisplayName("COMPLETED 최신순은 완료 운동 리포지토리를 날짜 내림차순으로 호출한다")
            void COMPLETED_최신순은_완료_운동_리포지토리를_날짜_내림차순으로_호출한다() {
                // given
                given(memberRepository.findById(myExerciseMember.getId()))
                        .willReturn(Optional.of(myExerciseMember));
                given(exerciseRepository.findMyCompletedExercisesWithPaging(eq(myExerciseMember.getId()), argThat(
                        pageable -> matchesSort(pageable, Sort.Direction.DESC, Sort.Direction.DESC))))
                        .willReturn(emptySlice(firstPage));

                // when
                exerciseMyQueryService.getMyExercises(
                        myExerciseMember.getId(), MyExerciseFilterType.COMPLETED, MyExerciseOrderType.LATEST, firstPage);

                // then
                verify(exerciseRepository, never()).findMyExercisesWithPaging(any(), any());
                verify(exerciseRepository, never()).findMyUpcomingExercisesWithPaging(any(), any());
                verify(exerciseRepository).findMyCompletedExercisesWithPaging(eq(myExerciseMember.getId()), any(Pageable.class));
            }

            @Test
            @DisplayName("ALL 오래된순은 전체 운동 리포지토리를 날짜 오름차순으로 호출한다")
            void ALL_오래된순은_전체_운동_리포지토리를_날짜_오름차순으로_호출한다() {
                // given
                given(memberRepository.findById(myExerciseMember.getId()))
                        .willReturn(Optional.of(myExerciseMember));
                given(exerciseRepository.findMyExercisesWithPaging(eq(myExerciseMember.getId()), argThat(
                        pageable -> matchesSort(pageable, Sort.Direction.ASC, Sort.Direction.ASC))))
                        .willReturn(emptySlice(firstPage));

                // when
                exerciseMyQueryService.getMyExercises(
                        myExerciseMember.getId(), MyExerciseFilterType.ALL, MyExerciseOrderType.OLDEST, firstPage);

                // then
                verify(exerciseRepository).findMyExercisesWithPaging(eq(myExerciseMember.getId()), any(Pageable.class));
            }

            @Test
            @DisplayName("UPCOMING 오래된순은 예정 운동 리포지토리를 날짜 내림차순으로 호출한다")
            void UPCOMING_오래된순은_예정_운동_리포지토리를_날짜_내림차순으로_호출한다() {
                // given
                given(memberRepository.findById(myExerciseMember.getId()))
                        .willReturn(Optional.of(myExerciseMember));
                given(exerciseRepository.findMyUpcomingExercisesWithPaging(eq(myExerciseMember.getId()), argThat(
                        pageable -> matchesSort(pageable, Sort.Direction.DESC, Sort.Direction.DESC))))
                        .willReturn(emptySlice(firstPage));

                // when
                exerciseMyQueryService.getMyExercises(
                        myExerciseMember.getId(), MyExerciseFilterType.UPCOMING, MyExerciseOrderType.OLDEST, firstPage);

                // then
                verify(exerciseRepository).findMyUpcomingExercisesWithPaging(eq(myExerciseMember.getId()), any(Pageable.class));
            }

            @Test
            @DisplayName("COMPLETED 오래된순은 완료 운동 리포지토리를 날짜 오름차순으로 호출한다")
            void COMPLETED_오래된순은_완료_운동_리포지토리를_날짜_오름차순으로_호출한다() {
                // given
                given(memberRepository.findById(myExerciseMember.getId()))
                        .willReturn(Optional.of(myExerciseMember));
                given(exerciseRepository.findMyCompletedExercisesWithPaging(eq(myExerciseMember.getId()), argThat(
                        pageable -> matchesSort(pageable, Sort.Direction.ASC, Sort.Direction.ASC))))
                        .willReturn(emptySlice(firstPage));

                // when
                exerciseMyQueryService.getMyExercises(
                        myExerciseMember.getId(), MyExerciseFilterType.COMPLETED, MyExerciseOrderType.OLDEST, firstPage);

                // then
                verify(exerciseRepository).findMyCompletedExercisesWithPaging(eq(myExerciseMember.getId()), any(Pageable.class));
            }

            @Test
            @DisplayName("조회된 운동이 없으면 빈 응답을 반환한다")
            void 조회된_운동이_없으면_빈_응답을_반환한다() {
                // given
                given(memberRepository.findById(myExerciseMember.getId()))
                        .willReturn(Optional.of(myExerciseMember));
                given(exerciseRepository.findMyExercisesWithPaging(eq(myExerciseMember.getId()), any(Pageable.class)))
                        .willReturn(emptySlice(firstPage));

                // when
                MyExerciseListDTO.Response response = exerciseMyQueryService.getMyExercises(
                        myExerciseMember.getId(), MyExerciseFilterType.ALL, MyExerciseOrderType.LATEST, firstPage);

                // then
                assertThat(response.totalCount()).isZero();
                assertThat(response.hasNext()).isFalse();
                assertThat(response.exercises()).isEmpty();
                verify(exerciseRepository, never()).findExerciseParticipantCountsByExerciseIds(any());
                verify(exerciseBookmarkRepository, never()).findAllExerciseIdsByMemberIdAndExerciseIds(any(), any());
            }

            @Test
            @DisplayName("조회 결과를 DTO 필드와 hasNext true로 매핑한다")
            void 조회_결과를_DTO_필드와_hasNext_true로_매핑한다() {
                // given
                Slice<Exercise> exerciseSlice = sliceOf(List.of(futureLatestExercise, completedExercise), true, firstPage);

                given(memberRepository.findById(myExerciseMember.getId()))
                        .willReturn(Optional.of(myExerciseMember));
                given(exerciseRepository.findMyExercisesWithPaging(eq(myExerciseMember.getId()), any(Pageable.class)))
                        .willReturn(exerciseSlice);
                given(exerciseRepository.findExerciseParticipantCountsByExerciseIds(
                        List.of(futureLatestExercise.getId(), completedExercise.getId())))
                        .willReturn(List.of(
                                new Object[]{futureLatestExercise.getId(), 3},
                                new Object[]{completedExercise.getId(), 1}
                        ));
                given(exerciseBookmarkRepository.findAllExerciseIdsByMemberIdAndExerciseIds(
                        myExerciseMember.getId(), List.of(futureLatestExercise.getId(), completedExercise.getId())))
                        .willReturn(List.of(futureLatestExercise.getId()));

                // when
                MyExerciseListDTO.Response response = exerciseMyQueryService.getMyExercises(
                        myExerciseMember.getId(), MyExerciseFilterType.ALL, MyExerciseOrderType.LATEST, firstPage);

                // then
                assertThat(response.totalCount()).isEqualTo(2);
                assertThat(response.hasNext()).isTrue();
                assertThat(response.exercises())
                        .extracting(
                                MyExerciseListDTO.ExerciseItem::exerciseId,
                                MyExerciseListDTO.ExerciseItem::partyId,
                                MyExerciseListDTO.ExerciseItem::partyName,
                                MyExerciseListDTO.ExerciseItem::isBookmarked,
                                MyExerciseListDTO.ExerciseItem::date,
                                MyExerciseListDTO.ExerciseItem::dayOfWeek,
                                MyExerciseListDTO.ExerciseItem::buildingName,
                                MyExerciseListDTO.ExerciseItem::startTime,
                                MyExerciseListDTO.ExerciseItem::endTime,
                                MyExerciseListDTO.ExerciseItem::currentParticipants,
                                MyExerciseListDTO.ExerciseItem::maxCapacity,
                                MyExerciseListDTO.ExerciseItem::isCompleted,
                                MyExerciseListDTO.ExerciseItem::partyGuestInviteAccept
                        )
                        .containsExactly(
                                tuple(703L, 10L, "테스트 모임", true,
                                        LocalDate.of(2099, 1, 10), "SATURDAY", "테스트 체육관",
                                        LocalTime.of(7, 30), LocalTime.of(9, 0), 3, 20, false, true),
                                tuple(701L, 10L, "테스트 모임", false,
                                        LocalDate.of(2024, 1, 5), "FRIDAY", "테스트 체육관",
                                        LocalTime.of(9, 0), LocalTime.of(11, 0), 1, 18, true, false)
                        );
            }

            @Test
            @DisplayName("조회 결과를 hasNext false로 매핑한다")
            void 조회_결과를_hasNext_false로_매핑한다() {
                // given
                Pageable secondPage = PageRequest.of(1, 1);
                Slice<Exercise> exerciseSlice = sliceOf(List.of(upcomingExercise), false, secondPage);

                given(memberRepository.findById(myExerciseMember.getId()))
                        .willReturn(Optional.of(myExerciseMember));
                given(exerciseRepository.findMyExercisesWithPaging(eq(myExerciseMember.getId()), any(Pageable.class)))
                        .willReturn(exerciseSlice);
                given(exerciseRepository.findExerciseParticipantCountsByExerciseIds(List.of(upcomingExercise.getId())))
                        .willReturn(List.of());
                given(exerciseBookmarkRepository.findAllExerciseIdsByMemberIdAndExerciseIds(
                        myExerciseMember.getId(), List.of(upcomingExercise.getId())))
                        .willReturn(List.of());

                // when
                MyExerciseListDTO.Response response = exerciseMyQueryService.getMyExercises(
                        myExerciseMember.getId(), MyExerciseFilterType.ALL, MyExerciseOrderType.LATEST, secondPage);

                // then
                assertThat(response.totalCount()).isEqualTo(1);
                assertThat(response.hasNext()).isFalse();
                assertThat(response.exercises().get(0).exerciseId()).isEqualTo(upcomingExercise.getId());
                assertThat(response.exercises().get(0).isCompleted()).isFalse();
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("존재하지 않는 멤버면 예외를 던진다")
            void 존재하지_않는_멤버면_예외를_던진다() {
                // given
                given(memberRepository.findById(999L))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> exerciseMyQueryService.getMyExercises(
                        999L, MyExerciseFilterType.ALL, MyExerciseOrderType.LATEST, firstPage))
                        .isInstanceOf(ExerciseException.class)
                        .hasFieldOrPropertyWithValue("code", ExerciseErrorCode.MEMBER_NOT_FOUND);
            }
        }

        private Exercise createMyExercise(long id, LocalDate date, LocalTime startTime,
                                          LocalTime endTime, int maxCapacity, boolean partyGuestAccept) {
            Exercise createdExercise = ExerciseFixture.createExerciseWithAddr(party, date, maxCapacity);
            ReflectionTestUtils.setField(createdExercise, "id", id);
            ReflectionTestUtils.setField(createdExercise, "startTime", startTime);
            ReflectionTestUtils.setField(createdExercise, "endTime", endTime);
            ReflectionTestUtils.setField(createdExercise, "partyGuestAccept", partyGuestAccept);
            return createdExercise;
        }

        private Slice<Exercise> emptySlice(Pageable pageable) {
            return new SliceImpl<>(List.of(), pageable, false);
        }

        private Slice<Exercise> sliceOf(List<Exercise> exercises, boolean hasNext, Pageable pageable) {
            return new SliceImpl<>(exercises, pageable, hasNext);
        }

        private boolean matchesSort(Pageable pageable, Sort.Direction dateDirection, Sort.Direction timeDirection) {
            if (pageable.getPageNumber() != firstPage.getPageNumber() || pageable.getPageSize() != firstPage.getPageSize()) {
                return false;
            }

            List<Sort.Order> orders = pageable.getSort().stream().toList();
            return orders.size() == 2
                   && orders.get(0).getProperty().equals("date")
                   && orders.get(0).getDirection() == dateDirection
                   && orders.get(1).getProperty().equals("startTime")
                   && orders.get(1).getDirection() == timeDirection;
        }
    }



}
