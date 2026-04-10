package umc.cockple.demo.domain.contest.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import umc.cockple.demo.domain.contest.domain.Contest;
import umc.cockple.demo.domain.contest.dto.*;
import umc.cockple.demo.domain.contest.enums.MedalType;
import umc.cockple.demo.domain.contest.exception.ContestErrorCode;
import umc.cockple.demo.domain.contest.repository.ContestRepository;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.party.enums.ParticipationType;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.IntegrationTestBase;
import umc.cockple.demo.support.SecurityContextHelper;
import umc.cockple.demo.support.fixture.ContestFixture;
import umc.cockple.demo.support.fixture.MemberFixture;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ContestIntegrationTest extends IntegrationTestBase {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired MemberRepository memberRepository;
    @Autowired ContestRepository contestRepository;

    private Member member;
    private Member otherMember;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(MemberFixture.createMember("테스터", Gender.MALE, Level.A, 1001L));
        otherMember = memberRepository.save(MemberFixture.createMember("다른회원", Gender.FEMALE, Level.B, 2002L));
    }

    @AfterEach
    void tearDown() {
        contestRepository.deleteAll();
        memberRepository.deleteAll();
        SecurityContextHelper.clearAuthentication();
    }

    private Contest createContest(Member m, String name, MedalType medalType) {
        return contestRepository.save(ContestFixture.createContest(m, name, medalType));
    }


    @Nested
    @DisplayName("POST /api/contests/my - 대회 기록 등록")
    class CreateContest {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("201 - 대회 기록을 등록하면 contestId, createdAt, 이미지/영상 ID 목록을 반환한다")
            void createContest_allFields() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                ContestRecordCreateDTO.Request request = ContestRecordCreateDTO.Request.builder()
                        .contestName("2026 전국 배드민턴 오픈 대회")
                        .date(LocalDate.of(2026, 6, 15))
                        .medalType(MedalType.GOLD)
                        .type(ParticipationType.SINGLE)
                        .level(Level.A)
                        .content("결승에서 승리했습니다.")
                        .contentIsOpen(true)
                        .videoIsOpen(true)
                        .contestImgs(List.of(
                                new AddContestImgRequest("images/contest1.jpg", 1),
                                new AddContestImgRequest("images/contest2.jpg", 2)
                        ))
                        .contestVideos(List.of(
                                new AddContestVideoRequest("videos/match.mp4", 1)
                        ))
                        .build();

                mockMvc.perform(post("/api/contests/my")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.code").value("COMMON201"))
                        .andExpect(jsonPath("$.data.contestId").isNumber())
                        .andExpect(jsonPath("$.data.createdAt").isNotEmpty())
                        .andExpect(jsonPath("$.data.contestImgIds").isArray())
                        .andExpect(jsonPath("$.data.contestImgIds", hasSize(2)))
                        .andExpect(jsonPath("$.data.contestVideoIds").isArray())
                        .andExpect(jsonPath("$.data.contestVideoIds", hasSize(1)));
            }

            @Test
            @DisplayName("201 - 이미지/영상 없이 필수 필드만으로 대회 기록을 등록하면 성공한다")
            void createContest_minimalFields() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                ContestRecordCreateDTO.Request request = ContestRecordCreateDTO.Request.builder()
                        .contestName("한강 배드민턴 클럽 대회")
                        .medalType(MedalType.NONE)
                        .type(ParticipationType.MEN_DOUBLES)
                        .level(Level.B)
                        .contentIsOpen(false)
                        .videoIsOpen(false)
                        .build();

                mockMvc.perform(post("/api/contests/my")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.contestId").isNumber())
                        .andExpect(jsonPath("$.data.contestImgIds", hasSize(0)))
                        .andExpect(jsonPath("$.data.contestVideoIds", hasSize(0)));
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("400 - contestName이 없으면 에러를 반환한다")
            void createContest_missingContestName() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                ContestRecordCreateDTO.Request request = ContestRecordCreateDTO.Request.builder()
                        .type(ParticipationType.SINGLE)
                        .level(Level.A)
                        .contentIsOpen(true)
                        .videoIsOpen(false)
                        .build();

                mockMvc.perform(post("/api/contests/my")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("400 - 이미지가 3개를 초과하면 에러를 반환한다")
            void createContest_imageLimitExceeded() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                ContestRecordCreateDTO.Request request = ContestRecordCreateDTO.Request.builder()
                        .contestName("테스트 대회")
                        .type(ParticipationType.SINGLE)
                        .level(Level.A)
                        .contentIsOpen(true)
                        .videoIsOpen(false)
                        .contestImgs(List.of(
                                new AddContestImgRequest("img1.jpg", 1),
                                new AddContestImgRequest("img2.jpg", 2),
                                new AddContestImgRequest("img3.jpg", 3),
                                new AddContestImgRequest("img4.jpg", 4)
                        ))
                        .build();

                mockMvc.perform(post("/api/contests/my")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("400 - ParticipationType이 없으면 에러를 반환한다")
            void createContest_missingType() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                ContestRecordCreateDTO.Request request = ContestRecordCreateDTO.Request.builder()
                        .contestName("테스트 대회")
                        .level(Level.A)
                        .contentIsOpen(true)
                        .videoIsOpen(false)
                        .build();

                mockMvc.perform(post("/api/contests/my")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("400 - content가 100자를 초과하면 에러를 반환한다")
            void createContest_contentTooLong() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                ContestRecordCreateDTO.Request request = ContestRecordCreateDTO.Request.builder()
                        .contestName("테스트 대회")
                        .type(ParticipationType.SINGLE)
                        .level(Level.A)
                        .content("a".repeat(101))
                        .contentIsOpen(true)
                        .videoIsOpen(false)
                        .build();

                mockMvc.perform(post("/api/contests/my")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("400 - level이 없으면 에러를 반환한다")
            void createContest_missingLevel() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                ContestRecordCreateDTO.Request request = ContestRecordCreateDTO.Request.builder()
                        .contestName("테스트 대회")
                        .type(ParticipationType.SINGLE)
                        .contentIsOpen(true)
                        .videoIsOpen(false)
                        .build();

                mockMvc.perform(post("/api/contests/my")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest());
            }
        }
    }


    @Nested
    @DisplayName("PATCH /api/contests/my/{contestId} - 대회 기록 수정")
    class UpdateContest {

        private Contest myContest;

        @BeforeEach
        void setUpContest() {
            myContest = createContest(member, "원래 대회 이름", MedalType.NONE);
        }

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("201 - 대회 기록을 수정하면 contestId, 이미지/영상 목록, updatedAt을 반환한다")
            void updateContest_allFields() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                ContestRecordUpdateDTO.Request request = new ContestRecordUpdateDTO.Request(
                        "수정된 대회 이름",
                        LocalDate.of(2026, 9, 20),
                        MedalType.SILVER,
                        ParticipationType.MIX_DOUBLES,
                        Level.B,
                        "수정된 대회 후기",
                        true,
                        false,
                        List.of(new ContestImgUpdateRequest(null, "new-img.jpg", 1)),
                        List.of(new ContestVideoUpdateRequest(null, "new-video.mp4", 1))
                );

                mockMvc.perform(patch("/api/contests/my/{contestId}", myContest.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.code").value("COMMON201"))
                        .andExpect(jsonPath("$.data.contestId").value(myContest.getId()))
                        .andExpect(jsonPath("$.data.contestImgs").isArray())
                        .andExpect(jsonPath("$.data.contestImgs", hasSize(1)))
                        .andExpect(jsonPath("$.data.contestImgs[0].imgKey").value("new-img.jpg"))
                        .andExpect(jsonPath("$.data.contestImgs[0].imgOrder").value(1))
                        .andExpect(jsonPath("$.data.contestVideos").isArray())
                        .andExpect(jsonPath("$.data.contestVideos", hasSize(1)))
                        .andExpect(jsonPath("$.data.updatedAt").isNotEmpty());
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("404 - 존재하지 않는 대회 기록을 수정하려 하면 에러를 반환한다")
            void updateContest_notFound() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                ContestRecordUpdateDTO.Request request = new ContestRecordUpdateDTO.Request(
                        "수정된 이름", null, MedalType.GOLD,
                        ParticipationType.SINGLE, Level.A, null, true, false,
                        null, null
                );

                mockMvc.perform(patch("/api/contests/my/{contestId}", 9999L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value(ContestErrorCode.CONTEST_NOT_FOUND.getCode()))
                        .andExpect(jsonPath("$.message").value(ContestErrorCode.CONTEST_NOT_FOUND.getMessage()));
            }

            @Test
            @DisplayName("404 - 다른 사람의 대회 기록을 수정하려 하면 에러를 반환한다")
            void updateContest_notOwner() throws Exception {
                SecurityContextHelper.setAuthentication(otherMember.getId(), otherMember.getNickname());

                ContestRecordUpdateDTO.Request request = new ContestRecordUpdateDTO.Request(
                        "수정된 이름", null, MedalType.GOLD,
                        ParticipationType.SINGLE, Level.A, null, true, false,
                        null, null
                );

                mockMvc.perform(patch("/api/contests/my/{contestId}", myContest.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value(ContestErrorCode.CONTEST_NOT_FOUND.getCode()));
            }
        }
    }


    @Nested
    @DisplayName("DELETE /api/contests/my/{contestId} - 대회 기록 삭제")
    class DeleteContest {

        private Contest myContest;

        @BeforeEach
        void setUpContest() {
            myContest = createContest(member, "삭제할 대회", MedalType.BRONZE);
        }

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("200 - 자신의 대회 기록을 삭제하면 삭제된 contestId를 반환한다")
            void deleteContest_success() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(delete("/api/contests/my/{contestId}", myContest.getId()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.code").value("COMMON200"))
                        .andExpect(jsonPath("$.data.deleteContestId").value(myContest.getId()));
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("404 - 존재하지 않는 대회 기록을 삭제하려 하면 에러를 반환한다")
            void deleteContest_notFound() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(delete("/api/contests/my/{contestId}", 9999L))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value(ContestErrorCode.CONTEST_NOT_FOUND.getCode()))
                        .andExpect(jsonPath("$.message").value(ContestErrorCode.CONTEST_NOT_FOUND.getMessage()));
            }

            @Test
            @DisplayName("404 - 다른 사람의 대회 기록을 삭제하려 하면 에러를 반환한다")
            void deleteContest_notOwner() throws Exception {
                SecurityContextHelper.setAuthentication(otherMember.getId(), otherMember.getNickname());

                mockMvc.perform(delete("/api/contests/my/{contestId}", myContest.getId()))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value(ContestErrorCode.CONTEST_NOT_FOUND.getCode()));
            }
        }
    }


    @Nested
    @DisplayName("GET /api/contests/my/{contestId} - 내 대회 기록 상세 조회")
    class GetMyContestRecordDetail {

        private Contest myContest;

        @BeforeEach
        void setUpContest() {
            myContest = createContest(member, "내 대회 기록", MedalType.GOLD);
        }

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("200 - 자신의 대회 기록을 상세 조회하면 모든 필드를 반환한다")
            void getMyContestRecordDetail_allFields() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/contests/my/{contestId}", myContest.getId()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.code").value("COMMON200"))
                        .andExpect(jsonPath("$.data.contestId").value(myContest.getId()))
                        .andExpect(jsonPath("$.data.contestName").value("내 대회 기록"))
                        .andExpect(jsonPath("$.data.date").value("2024-06-15"))
                        .andExpect(jsonPath("$.data.medalType").value("GOLD"))
                        .andExpect(jsonPath("$.data.type").value("SINGLE"))
                        .andExpect(jsonPath("$.data.level").value("A"))
                        .andExpect(jsonPath("$.data.content").value("대회 후기입니다."))
                        .andExpect(jsonPath("$.data.contentIsOpen").value(true))
                        .andExpect(jsonPath("$.data.videoIsOpen").value(true))
                        .andExpect(jsonPath("$.data.contestImgIds").isArray())
                        .andExpect(jsonPath("$.data.contestImgUrls").isArray())
                        .andExpect(jsonPath("$.data.contestVideoIds").isArray())
                        .andExpect(jsonPath("$.data.contestVideoUrls").isArray());
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("404 - 존재하지 않는 대회 기록을 조회하면 에러를 반환한다")
            void getMyContestRecordDetail_notFound() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/contests/my/{contestId}", 9999L))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value(ContestErrorCode.CONTEST_NOT_FOUND.getCode()))
                        .andExpect(jsonPath("$.message").value(ContestErrorCode.CONTEST_NOT_FOUND.getMessage()));
            }

            @Test
            @DisplayName("404 - 다른 사람의 대회 기록을 내 기록으로 조회하면 에러를 반환한다")
            void getMyContestRecordDetail_othersMember() throws Exception {
                Contest otherContest = createContest(otherMember, "타인의 대회", MedalType.SILVER);
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/contests/my/{contestId}", otherContest.getId()))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value(ContestErrorCode.CONTEST_NOT_FOUND.getCode()));
            }
        }
    }


    @Nested
    @DisplayName("GET /api/contests/my - 내 대회 기록 리스트 조회")
    class GetMyContestRecordList {

        @BeforeEach
        void setUpContests() {
            createContest(member, "금메달 대회", MedalType.GOLD);
            createContest(member, "은메달 대회", MedalType.SILVER);
            createContest(member, "동메달 대회", MedalType.BRONZE);
            createContest(member, "미입상 대회", MedalType.NONE);
        }

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("200 - medalType 없이 조회하면 전체 대회 기록 리스트와 모든 필드를 반환한다")
            void getMyContestRecords_allFields() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/contests/my"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.code").value("COMMON200"))
                        .andExpect(jsonPath("$.data", hasSize(4)))
                        .andExpect(jsonPath("$.data[0].contestId").isNumber())
                        .andExpect(jsonPath("$.data[0].contestName").isString())
                        .andExpect(jsonPath("$.data[0].type").value("SINGLE"))
                        .andExpect(jsonPath("$.data[0].level").value("A"))
                        .andExpect(jsonPath("$.data[0].date").value("2024-06-15"))
                        .andExpect(jsonPath("$.data[0].medalImgUrl").isString());
            }

            @Test
            @DisplayName("200 - medalType=GOLD로 조회하면 NONE 필터링 없이 전체를 반환한다")
            void getMyContestRecords_withGold() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/contests/my")
                                .param("medalType", "GOLD"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data", hasSize(4)));
            }

            @Test
            @DisplayName("200 - medalType=SILVER로 조회하면 NONE 필터링 없이 전체를 반환한다")
            void getMyContestRecords_withSilver() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/contests/my")
                                .param("medalType", "SILVER"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data", hasSize(4)));
            }

            @Test
            @DisplayName("200 - medalType=BRONZE로 조회하면 NONE 필터링 없이 전체를 반환한다")
            void getMyContestRecords_withBronze() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/contests/my")
                                .param("medalType", "BRONZE"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data", hasSize(4)));
            }

            @Test
            @DisplayName("200 - medalType=NONE으로 조회하면 미입상 대회 기록만 반환한다")
            void getMyContestRecords_filterByNone() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/contests/my")
                                .param("medalType", "NONE"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data", hasSize(1)))
                        .andExpect(jsonPath("$.data[0].contestName").value("미입상 대회"));
            }

            @Test
            @DisplayName("200 - 대회 기록이 없으면 빈 리스트를 반환한다")
            void getMyContestRecords_empty() throws Exception {
                SecurityContextHelper.setAuthentication(otherMember.getId(), otherMember.getNickname());

                mockMvc.perform(get("/api/contests/my"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data", hasSize(0)));
            }
        }
    }



    @Nested
    @DisplayName("GET /api/contests/my/medals - 내 메달 조회")
    class GetMyMedals {

        @BeforeEach
        void setUpContests() {
            createContest(member, "금메달 대회1", MedalType.GOLD);
            createContest(member, "금메달 대회2", MedalType.GOLD);
            createContest(member, "은메달 대회", MedalType.SILVER);
            createContest(member, "동메달 대회", MedalType.BRONZE);
            createContest(member, "미입상 대회", MedalType.NONE);
        }

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("200 - 메달 현황을 조회하면 총 개수(NONE 제외)와 종류별 개수를 반환한다")
            void getMyMedals_allFields() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/contests/my/medals"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.code").value("COMMON200"))
                        .andExpect(jsonPath("$.data.myMedalTotal").value(4))
                        .andExpect(jsonPath("$.data.goldCount").value(2))
                        .andExpect(jsonPath("$.data.silverCount").value(1))
                        .andExpect(jsonPath("$.data.bronzeCount").value(1));
            }

            @Test
            @DisplayName("200 - 메달이 없으면 모두 0을 반환한다")
            void getMyMedals_noMedals() throws Exception {
                SecurityContextHelper.setAuthentication(otherMember.getId(), otherMember.getNickname());

                mockMvc.perform(get("/api/contests/my/medals"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.myMedalTotal").value(0))
                        .andExpect(jsonPath("$.data.goldCount").value(0))
                        .andExpect(jsonPath("$.data.silverCount").value(0))
                        .andExpect(jsonPath("$.data.bronzeCount").value(0));
            }
        }
    }


    @Nested
    @DisplayName("GET /api/members/{memberId}/contests/{contestId} - 다른 사람의 대회 기록 상세 조회")
    class GetOtherMemberContestRecordDetail {

        private Contest otherContest;

        @BeforeEach
        void setUpContest() {
            otherContest = contestRepository.save(ContestFixture.createPrivateContest(
                    otherMember, "타인의 대회", MedalType.BRONZE,
                    LocalDate.of(2026, 3, 10), ParticipationType.WOMEN_DOUBLES, Level.C, "비공개 후기"
            ));
        }

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("200 - 비공개 설정 시 content와 영상은 숨기고 다른 사람의 대회 기록 상세를 반환한다")
            void getOtherContestDetail_allFields() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/members/{memberId}/contests/{contestId}",
                                otherMember.getId(), otherContest.getId()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.code").value("COMMON200"))
                        .andExpect(jsonPath("$.data.contestId").value(otherContest.getId()))
                        .andExpect(jsonPath("$.data.contestName").value("타인의 대회"))
                        .andExpect(jsonPath("$.data.date").value("2026-03-10"))
                        .andExpect(jsonPath("$.data.medalType").value("BRONZE"))
                        .andExpect(jsonPath("$.data.type").value("WOMEN_DOUBLES"))
                        .andExpect(jsonPath("$.data.level").value("C"))
                        .andExpect(jsonPath("$.data.contentIsOpen").value(false))
                        .andExpect(jsonPath("$.data.videoIsOpen").value(false))
                        .andExpect(jsonPath("$.data.content").value(""))
                        .andExpect(jsonPath("$.data.contestImgIds").isArray())
                        .andExpect(jsonPath("$.data.contestImgUrls").isArray())
                        .andExpect(jsonPath("$.data.contestVideoIds").isArray())
                        .andExpect(jsonPath("$.data.contestVideoUrls").isArray());
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("404 - 존재하지 않는 대회 기록을 조회하면 에러를 반환한다")
            void getOtherContestDetail_notFound() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/members/{memberId}/contests/{contestId}",
                                otherMember.getId(), 9999L))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value(ContestErrorCode.CONTEST_NOT_FOUND.getCode()))
                        .andExpect(jsonPath("$.message").value(ContestErrorCode.CONTEST_NOT_FOUND.getMessage()));
            }
        }
    }



    @Nested
    @DisplayName("GET /api/members/{memberId}/contests - 다른 사람의 대회 기록 리스트 조회")
    class GetOtherMemberContestList {

        @BeforeEach
        void setUpContests() {
            createContest(otherMember, "타인의 금메달 대회", MedalType.GOLD);
            createContest(otherMember, "타인의 미입상 대회", MedalType.NONE);
        }

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("200 - 다른 사람의 전체 대회 기록 리스트를 조회하면 성공한다")
            void getOtherContestList_all() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/members/{memberId}/contests", otherMember.getId()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.code").value("COMMON200"))
                        .andExpect(jsonPath("$.data", hasSize(2)));
            }

            @Test
            @DisplayName("200 - medalType=NONE으로 조회하면 다른 사람의 미입상 기록만 반환한다")
            void getOtherContestList_filterByNone() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/members/{memberId}/contests", otherMember.getId())
                                .param("medalType", "NONE"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data", hasSize(1)))
                        .andExpect(jsonPath("$.data[0].contestName").value("타인의 미입상 대회"));
            }
        }
    }


    @Nested
    @DisplayName("GET /api/members/{memberId}/medals - 다른 사람의 메달 조회")
    class GetOtherMemberMedals {

        @BeforeEach
        void setUpContests() {
            createContest(otherMember, "타인의 금메달", MedalType.GOLD);
            createContest(otherMember, "타인의 은메달", MedalType.SILVER);
        }

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("200 - 다른 사람의 메달 현황을 조회하면 종류별 개수를 반환한다")
            void getOtherMemberMedals_allFields() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/members/{memberId}/medals", otherMember.getId()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.code").value("COMMON200"))
                        .andExpect(jsonPath("$.data.myMedalTotal").value(2))
                        .andExpect(jsonPath("$.data.goldCount").value(1))
                        .andExpect(jsonPath("$.data.silverCount").value(1))
                        .andExpect(jsonPath("$.data.bronzeCount").value(0));
            }

            @Test
            @DisplayName("200 - 메달이 없는 회원을 조회하면 모두 0을 반환한다")
            void getOtherMemberMedals_noMedals() throws Exception {
                SecurityContextHelper.setAuthentication(otherMember.getId(), otherMember.getNickname());

                mockMvc.perform(get("/api/members/{memberId}/medals", member.getId()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.myMedalTotal").value(0))
                        .andExpect(jsonPath("$.data.goldCount").value(0))
                        .andExpect(jsonPath("$.data.silverCount").value(0))
                        .andExpect(jsonPath("$.data.bronzeCount").value(0));
            }
        }
    }
}
