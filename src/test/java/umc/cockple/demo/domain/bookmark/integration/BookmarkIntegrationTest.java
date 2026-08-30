package umc.cockple.demo.domain.bookmark.integration;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import umc.cockple.demo.domain.bookmark.domain.ExerciseBookmark;
import umc.cockple.demo.domain.bookmark.domain.PartyBookmark;
import umc.cockple.demo.domain.bookmark.enums.BookmarkedExerciseOrderType;
import umc.cockple.demo.domain.bookmark.exception.BookmarkErrorCode;
import umc.cockple.demo.domain.bookmark.repository.ExerciseBookmarkRepository;
import umc.cockple.demo.domain.bookmark.repository.PartyBookmarkRepository;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.domain.ExerciseAddr;
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.exercise.repository.MemberExerciseRepository;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.domain.PartyAddr;
import umc.cockple.demo.domain.party.enums.PartyOrderType;
import umc.cockple.demo.domain.party.exception.PartyErrorCode;
import umc.cockple.demo.domain.party.repository.PartyAddrRepository;
import umc.cockple.demo.domain.party.repository.PartyRepository;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.global.enums.Role;
import umc.cockple.demo.support.IntegrationTestBase;
import umc.cockple.demo.support.SecurityContextHelper;
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BookmarkIntegrationTest extends IntegrationTestBase {

    @Autowired MockMvc mockMvc;
    @Autowired MemberRepository memberRepository;
    @Autowired PartyRepository partyRepository;
    @Autowired PartyAddrRepository partyAddrRepository;
    @Autowired ExerciseRepository exerciseRepository;
    @Autowired ExerciseBookmarkRepository exerciseBookmarkRepository;
    @Autowired PartyBookmarkRepository partyBookmarkRepository;
    @Autowired MemberPartyRepository memberPartyRepository;
    @Autowired MemberExerciseRepository memberExerciseRepository;


    private Member member;
    private Party bookmarkParty;
    private Exercise bookmarkExercise;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(MemberFixture.createMember("테스터", Gender.MALE, Level.A, 1001L));

        PartyAddr addr = partyAddrRepository.save(PartyFixture.createPartyAddr("경기도", "안산시"));
        bookmarkParty = partyRepository.save(PartyFixture.createParty("테스트 모임", member.getId(), addr));

        memberPartyRepository.save(MemberFixture.createMemberParty(bookmarkParty, member, Role.PARTY_MANAGER));

        bookmarkExercise = exerciseRepository.save(Exercise.builder()
                .party(bookmarkParty)
                .date(LocalDate.of(2026, 12, 31))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(12, 0))
                .maxCapacity(10)
                .partyGuestAccept(true)
                .outsideGuestAccept(false)
                .gameHostId(bookmarkParty.getOwnerId())
                .exerciseAddr(ExerciseAddr.builder()
                        .addr1("경기도")
                        .addr2("안산시")
                        .streetAddr("경기도 안산시 한양대학로 1")
                        .buildingName("테스트 체육관")
                        .latitude(37.5)
                        .longitude(127.0)
                        .build())
                .build());
    }

    @AfterEach
    void tearDown() {
        exerciseBookmarkRepository.deleteAll();
        partyBookmarkRepository.deleteAll();
        memberExerciseRepository.deleteAll();
        exerciseRepository.deleteAll();
        memberPartyRepository.deleteAll();
        partyRepository.deleteAll();
        partyAddrRepository.deleteAll();
        memberRepository.deleteAll();
        SecurityContextHelper.clearAuthentication();
    }


    @Nested
    @DisplayName("POST /api/parties/{partyId}/bookmark - 모임 찜하기")
    class PartyBookmarkCreate {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("200 - 모임을 찜하면 partyBookmarkId를 반환한다")
            void partyBookmark_success() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(post("/api/parties/{partyId}/bookmark", bookmarkParty.getId()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data").isNumber());
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("404 - 존재하지 않는 모임이면 에러를 반환한다")
            void partyNotFound() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(post("/api/parties/{partyId}/bookmark", 999L))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value(PartyErrorCode.PARTY_NOT_FOUND.getCode()))
                        .andExpect(jsonPath("$.message").value(PartyErrorCode.PARTY_NOT_FOUND.getMessage()));
            }

            @Test
            @DisplayName("400 - 이미 찜한 모임이면 에러를 반환한다")
            void alreadyBookmarked() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());
                partyBookmarkRepository.save(PartyBookmark.builder()
                        .party(bookmarkParty)
                        .member(member)
                        .orderType(PartyOrderType.LATEST)
                        .build());

                mockMvc.perform(post("/api/parties/{partyId}/bookmark", bookmarkParty.getId()))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(BookmarkErrorCode.ALREADY_BOOKMARK.getCode()))
                        .andExpect(jsonPath("$.message").value(BookmarkErrorCode.ALREADY_BOOKMARK.getMessage()));
            }

            @Test
            @DisplayName("400 - 삭제된 모임은 찜할 수 없다")
            void partyIsDeleted() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());
                bookmarkParty.delete();
                partyRepository.save(bookmarkParty);

                mockMvc.perform(post("/api/parties/{partyId}/bookmark", bookmarkParty.getId()))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(PartyErrorCode.PARTY_IS_DELETED.getCode()))
                        .andExpect(jsonPath("$.message").value(PartyErrorCode.PARTY_IS_DELETED.getMessage()));
            }
        }
    }


    @Nested
    @DisplayName("DELETE /api/parties/{partyId}/bookmark - 모임 찜 해제")
    class PartyBookmarkRelease {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("200 - 찜한 모임을 해제하면 200 응답을 반환한다")
            void releasePartyBookmark_success() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());
                partyBookmarkRepository.save(PartyBookmark.builder()
                        .party(bookmarkParty)
                        .member(member)
                        .orderType(PartyOrderType.LATEST)
                        .build());

                mockMvc.perform(delete("/api/parties/{partyId}/bookmark", bookmarkParty.getId()))
                        .andExpect(status().isOk());
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("404 - 존재하지 않는 모임이면 에러를 반환한다")
            void partyNotFound() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(delete("/api/parties/{partyId}/bookmark", 999L))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value(PartyErrorCode.PARTY_NOT_FOUND.getCode()))
                        .andExpect(jsonPath("$.message").value(PartyErrorCode.PARTY_NOT_FOUND.getMessage()));
            }

            @Test
            @DisplayName("400 - 찜하지 않은 모임을 해제하려 하면 에러를 반환한다")
            void notBookmarked() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(delete("/api/parties/{partyId}/bookmark", bookmarkParty.getId()))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(BookmarkErrorCode.ALREADY_RELEASE_BOOKMARK.getCode()))
                        .andExpect(jsonPath("$.message").value(BookmarkErrorCode.ALREADY_RELEASE_BOOKMARK.getMessage()));
            }
        }
    }


    @Nested
    @DisplayName("POST /api/exercises/{exerciseId}/bookmark - 운동 찜하기")
    class ExerciseBookmarkCreate {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("200 - 운동을 찜하면 exerciseBookmarkId를 반환한다")
            void exerciseBookmark_success() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(post("/api/exercises/{exerciseId}/bookmark", bookmarkExercise.getId()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data").isNumber());
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("404 - 존재하지 않는 운동이면 에러를 반환한다")
            void exerciseNotFound() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(post("/api/exercises/{exerciseId}/bookmark", 999L))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value(ExerciseErrorCode.EXERCISE_NOT_FOUND.getCode()))
                        .andExpect(jsonPath("$.message").value(ExerciseErrorCode.EXERCISE_NOT_FOUND.getMessage()));
            }

            @Test
            @DisplayName("400 - 이미 찜한 운동이면 에러를 반환한다")
            void alreadyBookmarked() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());
                exerciseBookmarkRepository.save(ExerciseBookmark.builder()
                        .member(member)
                        .exercise(bookmarkExercise)
                        .build());

                mockMvc.perform(post("/api/exercises/{exerciseId}/bookmark", bookmarkExercise.getId()))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(BookmarkErrorCode.ALREADY_BOOKMARK.getCode()))
                        .andExpect(jsonPath("$.message").value(BookmarkErrorCode.ALREADY_BOOKMARK.getMessage()));
            }
        }
    }


    @Nested
    @DisplayName("DELETE /api/exercises/{exerciseId}/bookmark - 운동 찜 해제")
    class ExerciseBookmarkRelease {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("200 - 찜한 운동을 해제하면 200 응답을 반환한다")
            void releaseExerciseBookmark_success() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());
                exerciseBookmarkRepository.save(ExerciseBookmark.builder()
                        .member(member)
                        .exercise(bookmarkExercise)
                        .build());

                mockMvc.perform(delete("/api/exercises/{exerciseId}/bookmark", bookmarkExercise.getId()))
                        .andExpect(status().isOk());
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("404 - 존재하지 않는 운동이면 에러를 반환한다")
            void exerciseNotFound() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(delete("/api/exercises/{exerciseId}/bookmark", 999L))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value(ExerciseErrorCode.EXERCISE_NOT_FOUND.getCode()))
                        .andExpect(jsonPath("$.message").value(ExerciseErrorCode.EXERCISE_NOT_FOUND.getMessage()));
            }

            @Test
            @DisplayName("400 - 찜하지 않은 운동을 해제하려 하면 에러를 반환한다")
            void notBookmarked() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(delete("/api/exercises/{exerciseId}/bookmark", bookmarkExercise.getId()))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(BookmarkErrorCode.ALREADY_RELEASE_BOOKMARK.getCode()))
                        .andExpect(jsonPath("$.message").value(BookmarkErrorCode.ALREADY_RELEASE_BOOKMARK.getMessage()));
            }
        }
    }


    @Nested
    @DisplayName("GET /api/exercises/bookmarks - 찜한 운동 전체 조회")
    class GetAllExerciseBookmarks {

        private Exercise newExercise;

        @BeforeEach
        void setUp() {
            // 먼저 저장 = 오래된 북마크
            memberExerciseRepository.save(MemberFixture.createMemberExercise(member, bookmarkExercise));
            exerciseBookmarkRepository.save(ExerciseBookmark.builder()
                    .member(member)
                    .exercise(bookmarkExercise)
                    .build());

            // 나중에 저장 = 최신 북마크
            newExercise = exerciseRepository.save(Exercise.builder()
                    .party(bookmarkParty)
                    .date(LocalDate.of(2027, 6, 30))
                    .startTime(LocalTime.of(14, 0))
                    .endTime(LocalTime.of(16, 0))
                    .maxCapacity(8)
                    .partyGuestAccept(true)
                    .outsideGuestAccept(false)
                    .gameHostId(bookmarkParty.getOwnerId())
                    .exerciseAddr(ExerciseAddr.builder()
                            .addr1("경기도")
                            .addr2("안산시")
                            .streetAddr("경기도 안산시 한양대학로 1")
                            .buildingName("테스트 체육관")
                            .latitude(37.5)
                            .longitude(127.0)
                            .build())
                    .build());

            memberExerciseRepository.save(MemberFixture.createMemberExercise(member, newExercise));
            exerciseBookmarkRepository.save(ExerciseBookmark.builder()
                    .member(member)
                    .exercise(newExercise)
                    .build());
        }

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("200 - LATEST 정렬로 찜한 운동을 전체 조회하면 최신순으로 반환한다")
            void getAllExerciseBookmarks_latest_allFields() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/exercises/bookmarks")
                                .param("orderType", BookmarkedExerciseOrderType.LATEST.name()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data", hasSize(2)))
                        .andExpect(jsonPath("$.data[0].exerciseId").value(newExercise.getId()))
                        .andExpect(jsonPath("$.data[1].exerciseId").value(bookmarkExercise.getId()))
                        .andExpect(jsonPath("$.data[0].partyName").value("테스트 모임"))
                        .andExpect(jsonPath("$.data[0].buildingName").value("테스트 체육관"))
                        .andExpect(jsonPath("$.data[0].streetAddr").value("경기도 안산시 한양대학로 1"))
                        .andExpect(jsonPath("$.data[0].femaleLevel").isArray())
                        .andExpect(jsonPath("$.data[0].maleLevel").isArray())
                        .andExpect(jsonPath("$.data[0].date").value("2027-06-30"))
                        .andExpect(jsonPath("$.data[0].startExerciseTime").value("14:00:00"))
                        .andExpect(jsonPath("$.data[0].endExerciseTime").value("16:00:00"))
                        .andExpect(jsonPath("$.data[0].maxMemberCnt").value(8))
                        .andExpect(jsonPath("$.data[0].nowMemberCnt").isNumber())
                        .andExpect(jsonPath("$.data[0].includeParty").value(true))
                        .andExpect(jsonPath("$.data[0].includeExercise").value(true));
            }

            @Test
            @DisplayName("200 - EARLIEST 정렬로 찜한 운동을 전체 조회하면 오래된순으로 반환한다")
            void getAllExerciseBookmarks_earliest() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/exercises/bookmarks")
                                .param("orderType", BookmarkedExerciseOrderType.EARLIEST.name()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data", hasSize(2)))
                        .andExpect(jsonPath("$.data[0].exerciseId").value(bookmarkExercise.getId()))
                        .andExpect(jsonPath("$.data[1].exerciseId").value(newExercise.getId()));
            }

            @Test
            @DisplayName("200 - 찜한 운동이 없으면 빈 리스트를 반환한다")
            void noBookmarks_returnsEmptyList() throws Exception {
                Member otherMember = memberRepository.save(
                        MemberFixture.createMember("다른멤버", Gender.FEMALE, Level.B, 2002L));
                SecurityContextHelper.setAuthentication(otherMember.getId(), otherMember.getNickname());

                mockMvc.perform(get("/api/exercises/bookmarks")
                                .param("orderType", BookmarkedExerciseOrderType.LATEST.name()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data", hasSize(0)));
            }
        }
    }

    // ============================================================
    // GET /api/parties/bookmarks - 찜한 모임 전체 조회
    // ============================================================

    @Nested
    @DisplayName("GET /api/parties/bookmarks - 찜한 모임 전체 조회")
    class GetAllPartyBookmarks {

        private Party newParty;

        @BeforeEach
        void setUp() {
            // 먼저 저장 = 오래된 북마크
            partyBookmarkRepository.save(PartyBookmark.builder()
                    .party(bookmarkParty)
                    .member(member)
                    .orderType(PartyOrderType.LATEST)
                    .build());

            // 나중에 저장 = 최신 북마크
            PartyAddr newAddr = partyAddrRepository.save(PartyFixture.createPartyAddr("서울특별시", "강남구"));
            newParty = partyRepository.save(PartyFixture.createParty("새 테스트 모임", member.getId(), newAddr));

            partyBookmarkRepository.save(PartyBookmark.builder()
                    .party(newParty)
                    .member(member)
                    .orderType(PartyOrderType.LATEST)
                    .build());
        }

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("200 - LATEST 정렬로 찜한 모임을 전체 조회하면 최신순으로 반환한다")
            void getAllPartyBookmarks_latest_allFields() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/parties/bookmarks")
                                .param("orderType", PartyOrderType.LATEST.name()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data", hasSize(2)))
                        .andExpect(jsonPath("$.data[0].partyId").value(newParty.getId()))
                        .andExpect(jsonPath("$.data[1].partyId").value(bookmarkParty.getId()))
                        .andExpect(jsonPath("$.data[1].partyName").value("테스트 모임"))
                        .andExpect(jsonPath("$.data[1].addr1").value("경기도"))
                        .andExpect(jsonPath("$.data[1].addr2").value("안산시"))
                        .andExpect(jsonPath("$.data[1].maleLevel").isArray())
                        .andExpect(jsonPath("$.data[1].femaleLevel").isArray())
                        .andExpect(jsonPath("$.data[1].latestExerciseDate").value("2026-12-31"))
                        .andExpect(jsonPath("$.data[1].latestExerciseTime").value("MORNING"))
                        .andExpect(jsonPath("$.data[1].exerciseCnt").isNumber())
                        .andExpect(jsonPath("$.data[1].profileImgUrl").value(nullValue()));
            }

            @Test
            @DisplayName("200 - EXERCISE_COUNT 정렬로 찜한 모임 전체 조회 시 성공한다")
            void getAllPartyBookmarks_exerciseCount() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/parties/bookmarks")
                                .param("orderType", PartyOrderType.EXERCISE_COUNT.name()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data", hasSize(2)));
            }

            @Test
            @DisplayName("200 - OLDEST 정렬로 찜한 모임을 전체 조회하면 오래된순으로 반환한다")
            void getAllPartyBookmarks_oldest() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/parties/bookmarks")
                                .param("orderType", PartyOrderType.OLDEST.name()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data", hasSize(2)))
                        .andExpect(jsonPath("$.data[0].partyId").value(bookmarkParty.getId()))
                        .andExpect(jsonPath("$.data[1].partyId").value(newParty.getId()));
            }

            @Test
            @DisplayName("200 - 찜한 모임이 없으면 빈 리스트를 반환한다")
            void noBookmarks_returnsEmptyList() throws Exception {
                Member otherMember = memberRepository.save(
                        MemberFixture.createMember("다른멤버", Gender.FEMALE, Level.B, 2002L));
                SecurityContextHelper.setAuthentication(otherMember.getId(), otherMember.getNickname());

                mockMvc.perform(get("/api/parties/bookmarks")
                                .param("orderType", PartyOrderType.LATEST.name()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data", hasSize(0)));
            }
        }
    }
}
