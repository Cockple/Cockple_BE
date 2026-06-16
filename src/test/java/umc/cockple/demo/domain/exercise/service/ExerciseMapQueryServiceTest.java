package umc.cockple.demo.domain.exercise.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import umc.cockple.demo.domain.bookmark.repository.ExerciseBookmarkRepository;
import umc.cockple.demo.domain.exercise.converter.ExerciseConverter;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.dto.ExerciseBuildingDetailDTO;
import umc.cockple.demo.domain.exercise.dto.ExerciseMapBuildingsDTO;
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.exception.ExerciseException;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.exercise.service.query.ExerciseMapQueryService;
import umc.cockple.demo.domain.exercise.service.support.reader.ExerciseBookmarkReader;
import umc.cockple.demo.domain.exercise.service.support.reader.ExerciseReader;
import umc.cockple.demo.domain.file.service.FileService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberAddr;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.exception.MemberException;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.member.service.support.MemberLookupService;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.fixture.ExerciseFixture;
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExerciseMapQueryService")
class ExerciseMapQueryServiceTest {

    private ExerciseMapQueryService exerciseMapQueryService;

    @Mock private ExerciseRepository exerciseRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private ExerciseBookmarkRepository exerciseBookmarkRepository;
    @Mock private FileService fileService;

    private Party party;

    @BeforeEach
    void setUp() {
        ExerciseConverter exerciseConverter = new ExerciseConverter(fileService);
        exerciseMapQueryService = new ExerciseMapQueryService(
                new ExerciseReader(exerciseRepository),
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
    @DisplayName("getBuildingExerciseDetails")
    class GetBuildingExerciseDetails {

        private Member buildingMember;
        private LocalDate targetDate;
        private String buildingName;
        private String streetAddr;

        @BeforeEach
        void setUp() {
            buildingMember = MemberFixture.createMember("건물상세멤버", Gender.FEMALE, Level.B, 8001L,
                    LocalDate.of(2000, 1, 1));
            ReflectionTestUtils.setField(buildingMember, "id", 8L);

            targetDate = LocalDate.of(2026, 5, 10);
            buildingName = "콕플 타워";
            streetAddr = "서울특별시 강남구 테헤란로 10";
        }

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("해당 건물 운동이 없으면 메타데이터가 포함된 빈 응답을 반환한다")
            void 해당_건물_운동이_없으면_메타데이터가_포함된_빈_응답을_반환한다() {
                // given
                given(exerciseRepository.findExercisesByBuildingAndDate(buildingName, streetAddr, targetDate))
                        .willReturn(List.of());

                // when
                ExerciseBuildingDetailDTO.Response response = exerciseMapQueryService.getBuildingExerciseDetails(
                        buildingName, streetAddr, targetDate, buildingMember.getId());

                // then
                assertThat(response.date()).isEqualTo(targetDate);
                assertThat(response.dayOfWeek()).isEqualTo("SUNDAY");
                assertThat(response.buildingName()).isEqualTo(buildingName);
                assertThat(response.exercises()).isEmpty();
                verify(exerciseRepository).findExercisesByBuildingAndDate(buildingName, streetAddr, targetDate);
                verify(exerciseBookmarkRepository, never()).findAllExerciseIdsByMemberIdAndExerciseIds(any(), any());
            }

            @Test
            @DisplayName("운동 목록을 순서와 북마크 상태를 유지해 DTO로 반환한다")
            void 운동_목록을_순서와_북마크_상태를_유지해_DTO로_반환한다() {
                // given
                Exercise morningExercise = createBuildingExercise(801L, LocalTime.of(9, 0), LocalTime.of(11, 0));
                Exercise eveningExercise = createBuildingExercise(802L, LocalTime.of(19, 0), LocalTime.of(21, 0));
                given(exerciseRepository.findExercisesByBuildingAndDate(buildingName, streetAddr, targetDate))
                        .willReturn(List.of(morningExercise, eveningExercise));
                given(exerciseBookmarkRepository.findAllExerciseIdsByMemberIdAndExerciseIds(
                        buildingMember.getId(), List.of(morningExercise.getId(), eveningExercise.getId())))
                        .willReturn(List.of(eveningExercise.getId()));

                // when
                ExerciseBuildingDetailDTO.Response response = exerciseMapQueryService.getBuildingExerciseDetails(
                        buildingName, streetAddr, targetDate, buildingMember.getId());

                // then
                assertThat(response.date()).isEqualTo(targetDate);
                assertThat(response.dayOfWeek()).isEqualTo("SUNDAY");
                assertThat(response.buildingName()).isEqualTo(buildingName);
                assertThat(response.exercises())
                        .extracting(
                                ExerciseBuildingDetailDTO.ExerciseItem::exerciseId,
                                ExerciseBuildingDetailDTO.ExerciseItem::partyId,
                                ExerciseBuildingDetailDTO.ExerciseItem::partyName,
                                ExerciseBuildingDetailDTO.ExerciseItem::profileImageUrl,
                                ExerciseBuildingDetailDTO.ExerciseItem::isBookmarked,
                                ExerciseBuildingDetailDTO.ExerciseItem::startTime,
                                ExerciseBuildingDetailDTO.ExerciseItem::endTime
                        )
                        .containsExactly(
                                tuple(801L, 10L, "테스트 모임", null, false, LocalTime.of(9, 0), LocalTime.of(11, 0)),
                                tuple(802L, 10L, "테스트 모임", null, true, LocalTime.of(19, 0), LocalTime.of(21, 0))
                        );
                verify(exerciseRepository).findExercisesByBuildingAndDate(buildingName, streetAddr, targetDate);
                verify(exerciseBookmarkRepository).findAllExerciseIdsByMemberIdAndExerciseIds(
                        buildingMember.getId(), List.of(morningExercise.getId(), eveningExercise.getId()));
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {
        }

        private Exercise createBuildingExercise(long id, LocalTime startTime, LocalTime endTime) {
            Exercise buildingExercise = ExerciseFixture.createExerciseWithAddr(party, targetDate, 12);
            ReflectionTestUtils.setField(buildingExercise, "id", id);
            ReflectionTestUtils.setField(buildingExercise, "startTime", startTime);
            ReflectionTestUtils.setField(buildingExercise, "endTime", endTime);
            ReflectionTestUtils.setField(buildingExercise, "exerciseAddr",
                    ExerciseFixture.createExerciseAddr(buildingName, streetAddr));
            return buildingExercise;
        }
    }

    @Nested
    @DisplayName("getExerciseMapCalendarSummary")
    class GetExerciseMapCalendarSummary {

        private Member mapMember;
        private Member memberWithoutMainAddr;
        private MemberAddr mainAddr;
        private Double radiusKm;

        @BeforeEach
        void setUp() {
            mapMember = MemberFixture.createMember("지도멤버", Gender.MALE, Level.B, 9001L,
                    LocalDate.of(2000, 1, 1));
            ReflectionTestUtils.setField(mapMember, "id", 9L);

            mainAddr = MemberAddr.builder()
                    .member(mapMember)
                    .addr1("서울특별시")
                    .addr2("강남구")
                    .addr3("역삼동")
                    .streetAddr("서울특별시 강남구 테헤란로 1")
                    .buildingName("대표주소")
                    .latitude(37.501)
                    .longitude(127.039)
                    .isMain(true)
                    .build();
            ReflectionTestUtils.setField(mapMember, "addresses", List.of(mainAddr));

            memberWithoutMainAddr = MemberFixture.createMember("대표주소없음", Gender.FEMALE, Level.C, 9002L,
                    LocalDate.of(2001, 1, 1));
            ReflectionTestUtils.setField(memberWithoutMainAddr, "id", 10L);
            MemberAddr subAddr = MemberAddr.builder()
                    .member(memberWithoutMainAddr)
                    .addr1("서울특별시")
                    .addr2("송파구")
                    .addr3("잠실동")
                    .streetAddr("서울특별시 송파구 올림픽로 1")
                    .buildingName("서브주소")
                    .latitude(37.514)
                    .longitude(127.102)
                    .isMain(false)
                    .build();
            ReflectionTestUtils.setField(memberWithoutMainAddr, "addresses", List.of(subAddr));

            radiusKm = 3.9;
        }

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("date가 null이면 현재 월 범위와 대표주소 좌표로 조회한다")
            void date가_null이면_현재_월_범위와_대표주소_좌표로_조회한다() {
                // given
                YearMonth currentMonth = YearMonth.now();
                LocalDate monthStart = currentMonth.atDay(1);
                LocalDate monthEnd = currentMonth.atEndOfMonth();

                given(memberRepository.findMemberWithAddresses(mapMember.getId()))
                        .willReturn(Optional.of(mapMember));
                given(exerciseRepository.findExercisesByMonthAndRadius(
                        eq(monthStart),
                        eq(monthEnd),
                        eq(37.501),
                        eq(127.039),
                        eq(radiusKm)))
                        .willReturn(List.of());

                // when
                ExerciseMapBuildingsDTO.Response response = exerciseMapQueryService.getExerciseMapCalendarSummary(
                        createMapQuery(null, null, null, radiusKm), mapMember.getId());

                // then
                assertThat(response.year()).isEqualTo(currentMonth.getYear());
                assertThat(response.month()).isEqualTo(currentMonth.getMonthValue());
                assertThat(response.centerLatitude()).isEqualTo(mainAddr.getLatitude());
                assertThat(response.centerLongitude()).isEqualTo(mainAddr.getLongitude());
                assertThat(response.radiusKm()).isEqualTo(radiusKm);
                assertThat(response.buildings()).isEmpty();
            }

            @Test
            @DisplayName("명시 좌표가 있으면 대표주소 대신 해당 좌표와 소수 반경으로 조회한다")
            void 명시_좌표가_있으면_대표주소_대신_해당_좌표와_소수_반경으로_조회한다() {
                // given
                LocalDate targetDate = LocalDate.of(2026, 4, 15);
                LocalDate monthStart = LocalDate.of(2026, 4, 1);
                LocalDate monthEnd = LocalDate.of(2026, 4, 30);

                given(memberRepository.findMemberWithAddresses(mapMember.getId()))
                        .willReturn(Optional.of(mapMember));
                given(exerciseRepository.findExercisesByMonthAndRadius(
                        eq(monthStart),
                        eq(monthEnd),
                        eq(37.55),
                        eq(127.11),
                        eq(radiusKm)))
                        .willReturn(List.of());

                // when
                ExerciseMapBuildingsDTO.Response response = exerciseMapQueryService.getExerciseMapCalendarSummary(
                        createMapQuery(targetDate, 37.55, 127.11, radiusKm), mapMember.getId());

                // then
                assertThat(response.year()).isEqualTo(2026);
                assertThat(response.month()).isEqualTo(4);
                assertThat(response.centerLatitude()).isEqualTo(37.55);
                assertThat(response.centerLongitude()).isEqualTo(127.11);
                assertThat(response.radiusKm()).isEqualTo(radiusKm);
                assertThat(response.buildings()).isEmpty();
            }

            @Test
            @DisplayName("운동을 날짜별과 건물별로 그룹화해 응답을 만든다")
            void 운동을_날짜별과_건물별로_그룹화해_응답을_만든다() {
                // given
                LocalDate targetDate = LocalDate.of(2026, 4, 15);
                Exercise dayOneMorning = createMapExercise(901L, LocalDate.of(2026, 4, 3),
                        "A빌딩", "서울특별시 강남구 테헤란로 10", 37.501, 127.041, LocalTime.of(9, 0));
                Exercise dayOneEveningSameBuilding = createMapExercise(902L, LocalDate.of(2026, 4, 3),
                        "A빌딩", "서울특별시 강남구 테헤란로 10", 37.501, 127.041, LocalTime.of(19, 0));
                Exercise dayOneOtherBuilding = createMapExercise(903L, LocalDate.of(2026, 4, 3),
                        "B빌딩", "서울특별시 강남구 테헤란로 20", 37.502, 127.042, LocalTime.of(13, 0));
                Exercise dayTwoBuilding = createMapExercise(904L, LocalDate.of(2026, 4, 4),
                        "A빌딩", "서울특별시 강남구 테헤란로 10", 37.501, 127.041, LocalTime.of(10, 0));

                given(memberRepository.findMemberWithAddresses(mapMember.getId()))
                        .willReturn(Optional.of(mapMember));
                given(exerciseRepository.findExercisesByMonthAndRadius(
                        any(), any(), any(), any(), any()))
                        .willReturn(List.of(dayOneMorning, dayOneEveningSameBuilding, dayOneOtherBuilding, dayTwoBuilding));

                // when
                ExerciseMapBuildingsDTO.Response response = exerciseMapQueryService.getExerciseMapCalendarSummary(
                        createMapQuery(targetDate, null, null, radiusKm), mapMember.getId());

                // then
                assertThat(response.year()).isEqualTo(2026);
                assertThat(response.month()).isEqualTo(4);
                assertThat(response.centerLatitude()).isEqualTo(mainAddr.getLatitude());
                assertThat(response.centerLongitude()).isEqualTo(mainAddr.getLongitude());
                assertThat(response.radiusKm()).isEqualTo(radiusKm);
                assertThat(response.buildings().keySet())
                        .containsExactly(LocalDate.of(2026, 4, 3), LocalDate.of(2026, 4, 4));
                assertThat(response.buildings().get(LocalDate.of(2026, 4, 3)))
                        .extracting(
                                ExerciseMapBuildingsDTO.BuildingInfo::buildingName,
                                ExerciseMapBuildingsDTO.BuildingInfo::streetAddr,
                                ExerciseMapBuildingsDTO.BuildingInfo::latitude,
                                ExerciseMapBuildingsDTO.BuildingInfo::longitude
                        )
                        .containsExactlyInAnyOrder(
                                tuple("A빌딩", "서울특별시 강남구 테헤란로 10", 37.501, 127.041),
                                tuple("B빌딩", "서울특별시 강남구 테헤란로 20", 37.502, 127.042)
                        );
                assertThat(response.buildings().get(LocalDate.of(2026, 4, 4)))
                        .extracting(
                                ExerciseMapBuildingsDTO.BuildingInfo::buildingName,
                                ExerciseMapBuildingsDTO.BuildingInfo::streetAddr
                        )
                        .containsExactly(tuple("A빌딩", "서울특별시 강남구 테헤란로 10"));
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("존재하지 않는 멤버면 예외를 던진다")
            void 존재하지_않는_멤버면_예외를_던진다() {
                // given
                given(memberRepository.findMemberWithAddresses(999L))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> exerciseMapQueryService.getExerciseMapCalendarSummary(
                        createMapQuery(LocalDate.of(2026, 4, 1), null, null, radiusKm), 999L))
                        .isInstanceOf(MemberException.class)
                        .hasFieldOrPropertyWithValue("code", MemberErrorCode.MEMBER_NOT_FOUND);
            }

            @Test
            @DisplayName("대표주소가 없으면 예외를 던진다")
            void 대표주소가_없으면_예외를_던진다() {
                // given
                given(memberRepository.findMemberWithAddresses(memberWithoutMainAddr.getId()))
                        .willReturn(Optional.of(memberWithoutMainAddr));

                // when & then
                assertThatThrownBy(() -> exerciseMapQueryService.getExerciseMapCalendarSummary(
                        createMapQuery(LocalDate.of(2026, 4, 1), null, null, radiusKm), memberWithoutMainAddr.getId()))
                        .isInstanceOf(MemberException.class)
                        .hasFieldOrPropertyWithValue("code", MemberErrorCode.MAIN_ADDRESS_NULL);
            }

            @Test
            @DisplayName("대표주소가 없으면 명시 좌표가 있어도 예외를 던진다")
            void 대표주소가_없으면_명시_좌표가_있어도_예외를_던진다() {
                // given
                given(memberRepository.findMemberWithAddresses(memberWithoutMainAddr.getId()))
                        .willReturn(Optional.of(memberWithoutMainAddr));

                // when & then
                assertThatThrownBy(() -> exerciseMapQueryService.getExerciseMapCalendarSummary(
                        createMapQuery(LocalDate.of(2026, 4, 1), 37.5, 127.0, radiusKm), memberWithoutMainAddr.getId()))
                        .isInstanceOf(MemberException.class)
                        .hasFieldOrPropertyWithValue("code", MemberErrorCode.MAIN_ADDRESS_NULL);
            }

        }

        private ExerciseMapBuildingsDTO.Query createMapQuery(
                LocalDate date, Double latitude, Double longitude, Double radiusKm) {
            return ExerciseMapBuildingsDTO.Query.of(date, latitude, longitude, radiusKm);
        }

        private Exercise createMapExercise(long id, LocalDate date, String buildingName,
                                           String streetAddr, double latitude, double longitude,
                                           LocalTime startTime) {
            Exercise mapExercise = ExerciseFixture.createExerciseWithAddr(party, date, 12);
            ReflectionTestUtils.setField(mapExercise, "id", id);
            ReflectionTestUtils.setField(mapExercise, "startTime", startTime);
            ReflectionTestUtils.setField(mapExercise, "exerciseAddr",
                    ExerciseFixture.createExerciseAddr(buildingName, streetAddr, latitude, longitude));
            return mapExercise;
        }
    }


}
