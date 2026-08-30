package umc.cockple.demo.domain.exercise.integration;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import umc.cockple.demo.domain.bookmark.domain.ExerciseBookmark;
import umc.cockple.demo.domain.bookmark.repository.ExerciseBookmarkRepository;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.exercise.repository.GuestRepository;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberAddr;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.repository.MemberAddrRepository;
import umc.cockple.demo.domain.exercise.repository.MemberExerciseRepository;
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
import umc.cockple.demo.support.fixture.ExerciseFixture;
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.time.LocalDate;
import java.time.LocalTime;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExerciseMapIntegrationTest extends IntegrationTestBase {

    @Autowired MockMvc mockMvc;
    @Autowired MemberRepository memberRepository;
    @Autowired MemberAddrRepository memberAddrRepository;
    @Autowired PartyRepository partyRepository;
    @Autowired PartyAddrRepository partyAddrRepository;
    @Autowired MemberPartyRepository memberPartyRepository;
    @Autowired ExerciseRepository exerciseRepository;
    @Autowired MemberExerciseRepository memberExerciseRepository;
    @Autowired GuestRepository guestRepository;
    @Autowired ExerciseBookmarkRepository exerciseBookmarkRepository;

    private Member manager;
    private Member subManager;
    private Member normalMember;
    private Member outsider;
    private Party party;

    @BeforeEach
    void setUp() {
        manager = memberRepository.save(MemberFixture.createMember("모임장", Gender.MALE, Level.A, 1001L, LocalDate.of(2000, 1, 1)));
        subManager = memberRepository.save(MemberFixture.createMember("부모임장", Gender.FEMALE, Level.B, 1002L, LocalDate.of(2000, 1, 1)));
        normalMember = memberRepository.save(MemberFixture.createMember("일반멤버", Gender.MALE, Level.C, 1003L, LocalDate.of(2000, 1, 1)));
        outsider = memberRepository.save(MemberFixture.createMember("외부회원", Gender.FEMALE, Level.B, 1004L, LocalDate.of(2001, 1, 1)));

        PartyAddr addr = partyAddrRepository.save(PartyFixture.createPartyAddr("서울특별시", "강남구"));
        party = partyRepository.save(PartyFixture.createParty("테스트 모임", manager.getId(), addr));

        memberPartyRepository.save(MemberFixture.createMemberParty(party, manager, Role.PARTY_MANAGER));
        memberPartyRepository.save(MemberFixture.createMemberParty(party, subManager, Role.PARTY_SUBMANAGER));
        memberPartyRepository.save(MemberFixture.createMemberParty(party, normalMember, Role.PARTY_MEMBER));
    }

    @AfterEach
    void tearDown() {
        guestRepository.deleteAll();
        exerciseBookmarkRepository.deleteAll();
        memberExerciseRepository.deleteAll();
        exerciseRepository.deleteAll();
        memberPartyRepository.deleteAll();
        partyRepository.deleteAll();
        partyAddrRepository.deleteAll();
        memberAddrRepository.deleteAll();
        memberRepository.deleteAll();
        SecurityContextHelper.clearAuthentication();
    }

    @Nested
    @DisplayName("GET /api/buildings/exercises/{date} - 건물 운동 상세 조회")
    class GetBuildingExerciseDetails {

        private final LocalDate targetDate = LocalDate.of(2026, 5, 10);
        private final String targetBuildingName = "콕플 타워";
        private final String targetStreetAddr = "서울특별시 강남구 테헤란로 10";
        private Exercise morningExercise;
        private Exercise eveningExercise;

        @BeforeEach
        void setUp() {
            eveningExercise = saveBuildingExercise(targetBuildingName, targetStreetAddr,
                    targetDate, LocalTime.of(19, 0), LocalTime.of(21, 0));
            morningExercise = saveBuildingExercise(targetBuildingName, targetStreetAddr,
                    targetDate, LocalTime.of(9, 0), LocalTime.of(11, 0));

            exerciseBookmarkRepository.save(ExerciseBookmark.builder()
                    .member(normalMember)
                    .exercise(eveningExercise)
                    .build());

            saveBuildingExercise("다른 건물", targetStreetAddr,
                    targetDate, LocalTime.of(13, 0), LocalTime.of(15, 0));
            saveBuildingExercise(targetBuildingName, "서울특별시 강남구 테헤란로 99",
                    targetDate, LocalTime.of(16, 0), LocalTime.of(18, 0));
        }

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("같은 건물과 주소의 운동만 시작시간 오름차순으로 반환한다")
            void 같은_건물과_주소의_운동만_시작시간_오름차순으로_반환한다() throws Exception {
                SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

                mockMvc.perform(get("/api/buildings/exercises/{date}", targetDate)
                                .param("buildingName", targetBuildingName)
                                .param("streetAddr", targetStreetAddr))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.date").value("2026-05-10"))
                        .andExpect(jsonPath("$.data.dayOfWeek").value("SUNDAY"))
                        .andExpect(jsonPath("$.data.buildingName").value(targetBuildingName))
                        .andExpect(jsonPath("$.data.exercises.length()").value(2))
                        .andExpect(jsonPath("$.data.exercises[0].exerciseId").value(morningExercise.getId()))
                        .andExpect(jsonPath("$.data.exercises[0].partyId").value(party.getId()))
                        .andExpect(jsonPath("$.data.exercises[0].partyName").value("테스트 모임"))
                        .andExpect(jsonPath("$.data.exercises[0].profileImageUrl").isEmpty())
                        .andExpect(jsonPath("$.data.exercises[0].isBookmarked").value(false))
                        .andExpect(jsonPath("$.data.exercises[0].startTime").value("09:00:00"))
                        .andExpect(jsonPath("$.data.exercises[0].endTime").value("11:00:00"))
                        .andExpect(jsonPath("$.data.exercises[1].exerciseId").value(eveningExercise.getId()))
                        .andExpect(jsonPath("$.data.exercises[1].isBookmarked").value(true))
                        .andExpect(jsonPath("$.data.exercises[1].startTime").value("19:00:00"))
                        .andExpect(jsonPath("$.data.exercises[1].endTime").value("21:00:00"));
            }

            @Test
            @DisplayName("해당 건물 운동이 없으면 메타데이터가 포함된 빈 응답을 반환한다")
            void 해당_건물_운동이_없으면_메타데이터가_포함된_빈_응답을_반환한다() throws Exception {
                SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

                mockMvc.perform(get("/api/buildings/exercises/{date}", targetDate)
                                .param("buildingName", "없는 건물")
                                .param("streetAddr", targetStreetAddr))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.date").value("2026-05-10"))
                        .andExpect(jsonPath("$.data.dayOfWeek").value("SUNDAY"))
                        .andExpect(jsonPath("$.data.buildingName").value("없는 건물"))
                        .andExpect(jsonPath("$.data.exercises").isEmpty());
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("buildingName이 없으면 400을 반환한다")
            void buildingName이_없으면_400을_반환한다() throws Exception {
                SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

                mockMvc.perform(get("/api/buildings/exercises/{date}", targetDate)
                                .param("streetAddr", targetStreetAddr))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("streetAddr이 없으면 400을 반환한다")
            void streetAddr이_없으면_400을_반환한다() throws Exception {
                SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

                mockMvc.perform(get("/api/buildings/exercises/{date}", targetDate)
                                .param("buildingName", targetBuildingName))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("날짜 형식이 잘못되면 400을 반환한다")
            void 날짜_형식이_잘못되면_400을_반환한다() throws Exception {
                SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

                mockMvc.perform(get("/api/buildings/exercises/{date}", "invalid-date")
                                .param("buildingName", targetBuildingName)
                                .param("streetAddr", targetStreetAddr))
                        .andExpect(status().isBadRequest());
            }
        }

        private Exercise saveBuildingExercise(String buildingName, String streetAddr,
                                              LocalDate date, LocalTime startTime, LocalTime endTime) {
            Exercise buildingExercise = ExerciseFixture.createExerciseWithAddr(party, date, 12);
            ReflectionTestUtils.setField(buildingExercise, "exerciseAddr",
                    ExerciseFixture.createExerciseAddr(buildingName, streetAddr));
            ReflectionTestUtils.setField(buildingExercise, "startTime", startTime);
            ReflectionTestUtils.setField(buildingExercise, "endTime", endTime);
            return exerciseRepository.save(buildingExercise);
        }
    }

    @Nested
    @DisplayName("GET /api/buildings/map/monthly - 월간 운동 건물 지도 데이터 조회")
    class GetMonthlyExerciseBuildings {

        private final LocalDate targetDate = LocalDate.of(2026, 4, 15);
        private Member memberWithoutMainAddr;

        @BeforeEach
        void setUp() {
            saveMemberAddr(normalMember, "서울특별시", "강남구", "역삼동",
                    "서울특별시 강남구 테헤란로 1", "대표주소", 37.5, 127.0, true);

            memberWithoutMainAddr = memberRepository.save(
                    MemberFixture.createMember("대표주소없음", Gender.FEMALE, Level.B, 1010L, LocalDate.of(2000, 1, 1)));
            saveMemberAddr(memberWithoutMainAddr, "서울특별시", "송파구", "잠실동",
                    "서울특별시 송파구 올림픽로 1", "보조주소", 37.514, 127.102, false);

            saveMapExercise(LocalDate.of(2026, 4, 3), "A빌딩", "서울특별시 강남구 테헤란로 10",
                    37.5005, 127.0005, LocalTime.of(9, 0));
            saveMapExercise(LocalDate.of(2026, 4, 3), "A빌딩", "서울특별시 강남구 테헤란로 10",
                    37.5005, 127.0005, LocalTime.of(19, 0));
            saveMapExercise(LocalDate.of(2026, 4, 3), "B빌딩", "서울특별시 강남구 테헤란로 20",
                    37.501, 127.001, LocalTime.of(13, 0));
            saveMapExercise(LocalDate.of(2026, 4, 4), "A빌딩", "서울특별시 강남구 테헤란로 10",
                    37.5005, 127.0005, LocalTime.of(10, 0));
            saveMapExercise(LocalDate.of(2026, 4, 7), "소수반경빌딩", "서울특별시 강남구 테헤란로 390",
                    37.535, 127.0, LocalTime.of(15, 0));
            saveMapExercise(LocalDate.of(2026, 4, 5), "반경밖빌딩", "부산광역시 해운대구 센텀로 1",
                    35.17, 129.13, LocalTime.of(12, 0));
            saveMapExercise(LocalDate.of(2026, 4, 6), "부산빌딩", "부산광역시 해운대구 센텀로 2",
                    35.1705, 129.1305, LocalTime.of(14, 0));
        }

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("기본 요청은 현재 월과 대표주소 중심 좌표를 사용한다")
            void 기본_요청은_현재_월과_대표주소_중심_좌표를_사용한다() throws Exception {
                SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

                mockMvc.perform(get("/api/buildings/map/monthly"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.year").value(LocalDate.now().getYear()))
                        .andExpect(jsonPath("$.data.month").value(LocalDate.now().getMonthValue()))
                        .andExpect(jsonPath("$.data.centerLatitude").value(37.5))
                        .andExpect(jsonPath("$.data.centerLongitude").value(127.0))
                        .andExpect(jsonPath("$.data.radiusKm").value(3.0))
                        .andExpect(jsonPath("$.data.buildings").isMap());
            }

            @Test
            @DisplayName("명시 날짜와 좌표로 조회하면 날짜별 건물 지도를 dedupe하여 반환한다")
            void 명시_날짜와_좌표로_조회하면_날짜별_건물_지도를_dedupe하여_반환한다() throws Exception {
                SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

                mockMvc.perform(get("/api/buildings/map/monthly")
                                .param("date", targetDate.toString())
                                .param("latitude", "37.5")
                                .param("longitude", "127.0")
                                .param("radiusKm", "3.9"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.year").value(2026))
                        .andExpect(jsonPath("$.data.month").value(4))
                        .andExpect(jsonPath("$.data.centerLatitude").value(37.5))
                        .andExpect(jsonPath("$.data.centerLongitude").value(127.0))
                        .andExpect(jsonPath("$.data.radiusKm").value(3.9))
                        .andExpect(jsonPath("$.data.buildings['2026-04-03'].length()").value(2))
                        .andExpect(jsonPath("$.data.buildings['2026-04-03'][*].buildingName", containsInAnyOrder("A빌딩", "B빌딩")))
                        .andExpect(jsonPath("$.data.buildings['2026-04-04'].length()").value(1))
                        .andExpect(jsonPath("$.data.buildings['2026-04-04'][0].buildingName").value("A빌딩"))
                        .andExpect(jsonPath("$.data.buildings['2026-04-05']").doesNotExist());
            }

            @Test
            @DisplayName("3.0km 밖이지만 3.9km 안인 건물은 소수 반경을 절사하지 않고 반환한다")
            void 삼키로미터_밖_삼점구키로미터_안_건물은_소수_반경을_절사하지_않고_반환한다() throws Exception {
                SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

                mockMvc.perform(get("/api/buildings/map/monthly")
                                .param("date", targetDate.toString())
                                .param("latitude", "37.5")
                                .param("longitude", "127.0")
                                .param("radiusKm", "3.9"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.buildings['2026-04-07'][0].buildingName").value("소수반경빌딩"));

                mockMvc.perform(get("/api/buildings/map/monthly")
                                .param("date", targetDate.toString())
                                .param("latitude", "37.5")
                                .param("longitude", "127.0")
                                .param("radiusKm", "3.0"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.buildings['2026-04-07']").doesNotExist());
            }

            @Test
            @DisplayName("명시 좌표는 대표주소 대신 응답 중심 좌표로 반영된다")
            void 명시_좌표는_대표주소_대신_응답_중심_좌표로_반영된다() throws Exception {
                SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

                mockMvc.perform(get("/api/buildings/map/monthly")
                                .param("date", targetDate.toString())
                                .param("latitude", "35.17")
                                .param("longitude", "129.13")
                                .param("radiusKm", "5.0"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.centerLatitude").value(35.17))
                        .andExpect(jsonPath("$.data.centerLongitude").value(129.13))
                        .andExpect(jsonPath("$.data.radiusKm").value(5.0))
                        .andExpect(jsonPath("$.data.buildings['2026-04-06'][0].buildingName").value("부산빌딩"));
            }

            @Test
            @DisplayName("MySQL POINT는 longitude latitude 순서로 생성되어 부산 좌표가 서울 결과와 섞이지 않는다")
            void mysql_point는_longitude_latitude_순서로_생성되어_부산_좌표가_서울_결과와_섞이지_않는다() throws Exception {
                SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

                mockMvc.perform(get("/api/buildings/map/monthly")
                                .param("date", targetDate.toString())
                                .param("latitude", "35.17")
                                .param("longitude", "129.13")
                                .param("radiusKm", "1.0"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.centerLatitude").value(35.17))
                        .andExpect(jsonPath("$.data.centerLongitude").value(129.13))
                        .andExpect(jsonPath("$.data.buildings['2026-04-05'][0].buildingName").value("반경밖빌딩"))
                        .andExpect(jsonPath("$.data.buildings['2026-04-06'][0].buildingName").value("부산빌딩"))
                        .andExpect(jsonPath("$.data.buildings['2026-04-03']").doesNotExist())
                        .andExpect(jsonPath("$.data.buildings['2026-04-04']").doesNotExist())
                        .andExpect(jsonPath("$.data.buildings['2026-04-07']").doesNotExist());
            }

            @Test
            @DisplayName("반경 내 운동이 없으면 빈 buildings를 반환한다")
            void 반경_내_운동이_없으면_빈_buildings를_반환한다() throws Exception {
                SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

                mockMvc.perform(get("/api/buildings/map/monthly")
                                .param("date", targetDate.toString())
                                .param("latitude", "36.0")
                                .param("longitude", "128.0")
                                .param("radiusKm", "1.0"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.year").value(2026))
                        .andExpect(jsonPath("$.data.month").value(4))
                        .andExpect(jsonPath("$.data.buildings").isEmpty());
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("존재하지 않는 멤버면 에러를 반환한다")
            void 존재하지_않는_멤버면_에러를_반환한다() throws Exception {
                SecurityContextHelper.setAuthentication(999L, "없는멤버");

                mockMvc.perform(get("/api/buildings/map/monthly")
                                .param("date", targetDate.toString()))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value(MemberErrorCode.MEMBER_NOT_FOUND.getCode()))
                        .andExpect(jsonPath("$.message").value(MemberErrorCode.MEMBER_NOT_FOUND.getMessage()));
            }

            @Test
            @DisplayName("대표주소가 없으면 에러를 반환한다")
            void 대표주소가_없으면_에러를_반환한다() throws Exception {
                SecurityContextHelper.setAuthentication(memberWithoutMainAddr.getId(), memberWithoutMainAddr.getNickname());

                mockMvc.perform(get("/api/buildings/map/monthly")
                                .param("date", targetDate.toString()))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(MemberErrorCode.MAIN_ADDRESS_NULL.getCode()))
                        .andExpect(jsonPath("$.message").value(MemberErrorCode.MAIN_ADDRESS_NULL.getMessage()));
            }

            @Test
            @DisplayName("대표주소가 없으면 명시 좌표가 있어도 에러를 반환한다")
            void 대표주소가_없으면_명시_좌표가_있어도_에러를_반환한다() throws Exception {
                SecurityContextHelper.setAuthentication(memberWithoutMainAddr.getId(), memberWithoutMainAddr.getNickname());

                mockMvc.perform(get("/api/buildings/map/monthly")
                                .param("date", targetDate.toString())
                                .param("latitude", "37.5")
                                .param("longitude", "127.0"))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(MemberErrorCode.MAIN_ADDRESS_NULL.getCode()))
                        .andExpect(jsonPath("$.message").value(MemberErrorCode.MAIN_ADDRESS_NULL.getMessage()));
            }

            @Test
            @DisplayName("위도만 주면 에러를 반환한다")
            void 위도만_주면_에러를_반환한다() throws Exception {
                SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

                mockMvc.perform(get("/api/buildings/map/monthly")
                                .param("date", targetDate.toString())
                                .param("latitude", "37.5"))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(ExerciseErrorCode.INCOMPLETE_LOCATION_INFO.getCode()))
                        .andExpect(jsonPath("$.message").value(ExerciseErrorCode.INCOMPLETE_LOCATION_INFO.getMessage()));
            }

            @Test
            @DisplayName("좌표 범위를 벗어나면 400을 반환한다")
            void 좌표_범위를_벗어나면_400을_반환한다() throws Exception {
                SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

                mockMvc.perform(get("/api/buildings/map/monthly")
                                .param("date", targetDate.toString())
                                .param("latitude", "91.0")
                                .param("longitude", "127.0"))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(ExerciseErrorCode.INVALID_LOCATION_INFO.getCode()))
                        .andExpect(jsonPath("$.message").value(ExerciseErrorCode.INVALID_LOCATION_INFO.getMessage()));
            }

            @Test
            @DisplayName("반경이 양수가 아니면 400을 반환한다")
            void 반경이_양수가_아니면_400을_반환한다() throws Exception {
                SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

                mockMvc.perform(get("/api/buildings/map/monthly")
                                .param("date", targetDate.toString())
                                .param("latitude", "37.5")
                                .param("longitude", "127.0")
                                .param("radiusKm", "0"))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(ExerciseErrorCode.INVALID_LOCATION_INFO.getCode()))
                        .andExpect(jsonPath("$.message").value(ExerciseErrorCode.INVALID_LOCATION_INFO.getMessage()));
            }

            @Test
            @DisplayName("날짜 형식이 잘못되면 400을 반환한다")
            void 날짜_형식이_잘못되면_400을_반환한다() throws Exception {
                SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

                mockMvc.perform(get("/api/buildings/map/monthly")
                                .param("date", "invalid-date"))
                        .andExpect(status().isBadRequest());
            }
        }

        private MemberAddr saveMemberAddr(Member member, String addr1, String addr2, String addr3,
                                          String streetAddr, String buildingName,
                                          double latitude, double longitude, boolean isMain) {
            return memberAddrRepository.save(MemberAddr.builder()
                    .member(member)
                    .addr1(addr1)
                    .addr2(addr2)
                    .addr3(addr3)
                    .streetAddr(streetAddr)
                    .buildingName(buildingName)
                    .latitude(latitude)
                    .longitude(longitude)
                    .isMain(isMain)
                    .build());
        }

        private Exercise saveMapExercise(LocalDate date, String buildingName, String streetAddr,
                                         double latitude, double longitude, LocalTime startTime) {
            Exercise exercise = ExerciseFixture.createExerciseWithAddr(party, date, 12);
            ReflectionTestUtils.setField(exercise, "exerciseAddr",
                    ExerciseFixture.createExerciseAddr(buildingName, streetAddr, latitude, longitude));
            ReflectionTestUtils.setField(exercise, "startTime", startTime);
            return exerciseRepository.save(exercise);
        }
    }
}
