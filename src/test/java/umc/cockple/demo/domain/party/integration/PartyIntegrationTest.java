package umc.cockple.demo.domain.party.integration;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.chat.domain.ChatRoom;
import umc.cockple.demo.domain.chat.domain.ChatRoomMember;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomRepository;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberAddr;
import umc.cockple.demo.domain.member.repository.MemberAddrRepository;
import umc.cockple.demo.domain.member.repository.MemberExerciseRepository;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.domain.PartyAddr;
import umc.cockple.demo.domain.party.enums.ParticipationType;
import umc.cockple.demo.domain.party.enums.ActivityTime;
import umc.cockple.demo.domain.party.exception.PartyErrorCode;
import umc.cockple.demo.domain.party.repository.PartyAddrRepository;
import umc.cockple.demo.domain.party.repository.PartyJoinRequestRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PartyIntegrationTest extends IntegrationTestBase {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    MemberRepository memberRepository;
    @Autowired
    PartyRepository partyRepository;
    @Autowired
    MemberPartyRepository memberPartyRepository;
    @Autowired
    PartyAddrRepository partyAddrRepository;
    @Autowired
    ExerciseRepository exerciseRepository;
    @Autowired
    MemberExerciseRepository memberExerciseRepository;
    @Autowired
    MemberAddrRepository memberAddrRepository;
    @Autowired
    ChatRoomRepository chatRoomRepository;
    @Autowired
    ChatRoomMemberRepository chatRoomMemberRepository;
    @Autowired
    PartyJoinRequestRepository partyJoinRequestRepository;

    private Member manager;
    private Member normalMember;
    private Party party;

    @BeforeEach
    void setUp() {
        manager = memberRepository
                .save(MemberFixture.createMember("매니저", Gender.MALE, Level.A, 1001L, LocalDate.of(1995, 1, 1)));
        memberAddrRepository.save(MemberAddr.builder()
                .member(manager)
                .addr1("서울특별시")
                .addr2("강남구")
                .addr3("역삼동")
                .streetAddr("테헤란로")
                .latitude(37.5)
                .longitude(127.0)
                .isMain(true)
                .build());

        normalMember = memberRepository.save(MemberFixture.createMember("일반멤버", Gender.FEMALE, Level.B, 1002L));

        PartyAddr addr = partyAddrRepository.save(PartyFixture.createPartyAddr("서울특별시", "강남구"));
        party = partyRepository.save(PartyFixture.createParty("테스트 모임", manager.getId(), addr));

        memberPartyRepository.save(MemberFixture.createMemberParty(party, manager, Role.party_MANAGER));
        memberPartyRepository.save(MemberFixture.createMemberParty(party, normalMember, Role.party_MEMBER));

        // 채팅방 생성 및 멤버 추가
        ChatRoom chatRoom = chatRoomRepository.save(ChatRoom.createPartyChatRoom(party));
        chatRoomMemberRepository.save(ChatRoomMember.create(chatRoom, manager));
        chatRoomMemberRepository.save(ChatRoomMember.create(chatRoom, normalMember));

        // 추천 조회용 모임 (manager가 가입하지 않은 모임)
        Party suggestedParty = PartyFixture.createParty("추천 모임", normalMember.getId(), addr);
        suggestedParty.addLevel(Gender.MALE, Level.A); // manager의 조건에 맞춤
        partyRepository.save(suggestedParty);

        SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());
    }

    @AfterEach
    void tearDown() {
        memberExerciseRepository.deleteAll();
        exerciseRepository.deleteAll();
        chatRoomMemberRepository.deleteAll();
        chatRoomRepository.deleteAll();
        partyJoinRequestRepository.deleteAll();
        memberPartyRepository.deleteAll();
        partyRepository.deleteAll();
        partyAddrRepository.deleteAll();
        memberAddrRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Nested
    @DisplayName("GET /api/parties/{partyId}/members - 모임 멤버 조회")
    class GetPartyMembers {

        @Test
        @DisplayName("200 - 모임의 멤버들을 역할별로 성공적으로 조회한다.")
        void success() throws Exception {
            // 부모임장 추가
            Member subManager = memberRepository.save(MemberFixture.createMember("부매니저", Gender.MALE, Level.A, 1003L));
            memberPartyRepository.save(MemberFixture.createMemberParty(party, subManager, Role.party_SUBMANAGER));

            // 모임장이 가입된 상태
            SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

            mockMvc.perform(get("/api/parties/{partyId}/members", party.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.summary.totalCount").value(3))
                    .andExpect(jsonPath("$.data.members[0].role").value("party_MANAGER"))
                    .andExpect(jsonPath("$.data.members[0].isMe").value(true))
                    .andExpect(jsonPath("$.data.members[1].role").value("party_SUBMANAGER"))
                    .andExpect(jsonPath("$.data.members[1].isMe").value(false))
                    .andExpect(jsonPath("$.data.members[2].role").value("party_MEMBER"))
                    .andExpect(jsonPath("$.data.members[2].isMe").value(false));
        }

        @Test
        @DisplayName("200 - 멤버 목록과 마지막 운동일을 정상 반환한다")
        void success_withLastExerciseDate() throws Exception {
            Exercise exercise = exerciseRepository.save(
                    ExerciseFixture.createExercise(party, LocalDate.of(2025, 1, 10)));
            memberExerciseRepository.save(MemberFixture.createMemberExercise(normalMember, exercise));

            mockMvc.perform(get("/api/parties/{partyId}/members", party.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.summary.totalCount").value(2))
                    .andExpect(jsonPath("$.data.summary.maleCount").value(1))
                    .andExpect(jsonPath("$.data.summary.femaleCount").value(1))
                    // 첫 번째 멤버(매니저) 전체 필드 검증
                    .andExpect(jsonPath("$.data.members[0].memberId").value(manager.getId()))
                    .andExpect(jsonPath("$.data.members[0].nickname").value("매니저"))
                    .andExpect(jsonPath("$.data.members[0].profileImageUrl").doesNotExist())
                    .andExpect(jsonPath("$.data.members[0].role").value("party_MANAGER"))
                    .andExpect(jsonPath("$.data.members[0].gender").value("MALE"))
                    .andExpect(jsonPath("$.data.members[0].level").value("A조"))
                    .andExpect(jsonPath("$.data.members[0].isMe").value(true))
                    .andExpect(jsonPath("$.data.members[0].lastExerciseDate").doesNotExist())
                    // 두 번째 멤버(일반멤버) 마지막 운동일 검증
                    .andExpect(jsonPath("$.data.members[1].lastExerciseDate").value("2025-01-10"));
        }

        @Test
        @DisplayName("200 - 운동 기록이 없는 멤버의 lastExerciseDate는 null이다")
        void success_noExerciseHistory() throws Exception {
            mockMvc.perform(get("/api/parties/{partyId}/members", party.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.summary.totalCount").value(2))
                    .andExpect(jsonPath("$.data.members[0].lastExerciseDate").isEmpty())
                    .andExpect(jsonPath("$.data.members[1].lastExerciseDate").isEmpty());
        }

        @Test
        @DisplayName("404 - 존재하지 않는 파티면 에러를 반환한다")
        void fail_partyNotFound() throws Exception {
            mockMvc.perform(get("/api/parties/{partyId}/members", 999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(PartyErrorCode.PARTY_NOT_FOUND.getCode()));
        }

        @Test
        @DisplayName("400 - 비활성화된 파티면 에러를 반환한다")
        void fail_partyInactive() throws Exception {
            party.delete();
            partyRepository.save(party);

            mockMvc.perform(get("/api/parties/{partyId}/members", party.getId()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(PartyErrorCode.PARTY_IS_DELETED.getCode()));
        }
    }

    @Nested
    @DisplayName("DELETE /api/parties/{partyId}/members/my - 모임 탈퇴")
    class LeaveParty {

        @Test
        @DisplayName("200 - 일반 멤버가 모임을 성공적으로 탈퇴한다")
        void success_leaveParty() throws Exception {
            // DB에서 최신 정보 보장
            Member member = memberRepository.findById(normalMember.getId()).orElseThrow();
            Party targetParty = partyRepository.findById(party.getId()).orElseThrow();

            // normalMember 세션으로 설정
            SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

            mockMvc.perform(delete("/api/parties/{partyId}/members/my", targetParty.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("COMMON200"));

            // DB에서 제거되었는지 확인
            boolean exists = memberPartyRepository.existsByPartyAndMember(targetParty, member);
            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("403 - 모임장은 탈퇴할 수 없다")
        void fail_leaveParty_owner() throws Exception {
            // manager(모임장) 세션으로 설정
            SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

            mockMvc.perform(delete("/api/parties/{partyId}/members/my", party.getId()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(PartyErrorCode.INVALID_ACTION_FOR_OWNER.getCode()));
        }

        @Test
        @DisplayName("403 - 부모임장은 탈퇴할 수 없다")
        void fail_leaveParty_subOwner() throws Exception {
            // 부모임장 생성 및 가입
            Member subManager = memberRepository.save(MemberFixture.createMember("부매니저", Gender.MALE, Level.A, 3001L));
            memberPartyRepository.save(MemberFixture.createMemberParty(party, subManager, Role.party_SUBMANAGER));

            // 부모임장 세션으로 설정
            SecurityContextHelper.setAuthentication(subManager.getId(), subManager.getNickname());

            mockMvc.perform(delete("/api/parties/{partyId}/members/my", party.getId()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(PartyErrorCode.INVALID_ACTION_FOR_SUBOWNER.getCode()));
        }

        @Test
        @DisplayName("400 - 해당 모임의 멤버가 아니면 탈퇴할 수 없다")
        void fail_leaveParty_notMember() throws Exception {
            // 가입하지 않은 새로운 멤버 생성
            Member nonMember = memberRepository.save(MemberFixture.createMember("외부인", Gender.MALE, Level.A, 4002L));
            SecurityContextHelper.setAuthentication(nonMember.getId(), nonMember.getNickname());

            mockMvc.perform(delete("/api/parties/{partyId}/members/my", party.getId()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(PartyErrorCode.NOT_MEMBER.getCode()));
        }
    }

    @Nested
    @DisplayName("GET /api/my/parties - 내 모임 조회")
    class GetMyParties {

        @Test
        @DisplayName("200 - 사용자가 가입한 모임 목록을 페이징하여 반환한다")
        void success_getMyParties() throws Exception {
            mockMvc.perform(get("/api/my/parties")
                            .param("created", "false")
                            .param("sort", "최신순")
                            .param("size", "10")
                            .param("page", "0"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("COMMON200"))
                    .andExpect(jsonPath("$.message").value("요청에 성공했습니다."))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].partyName").value("테스트 모임"))
                    .andExpect(jsonPath("$.data.content[0].partyId").value(party.getId()))
                    .andExpect(jsonPath("$.data.pageable.pageNumber").value(0))
                    .andExpect(jsonPath("$.data.last").value(true));
        }

        @Test
        @DisplayName("200 - 가입한 모임이 없을 경우 빈 목록을 반환한다")
        void success_emptyMyParties() throws Exception {
            Member newMember = memberRepository.save(umc.cockple.demo.support.fixture.MemberFixture.createMember("뉴비",
                    umc.cockple.demo.global.enums.Gender.MALE, umc.cockple.demo.global.enums.Level.BEGINNER, 3003L));
            SecurityContextHelper.setAuthentication(newMember.getId(), newMember.getNickname());

            mockMvc.perform(get("/api/my/parties")
                            .param("created", "false")
                            .param("sort", "최신순")
                            .param("size", "10")
                            .param("page", "0"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("COMMON200"))
                    .andExpect(jsonPath("$.message").value("요청에 성공했습니다."))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content").isEmpty())
                    .andExpect(jsonPath("$.data.empty").value(true));
        }
    }

    @Nested
    @DisplayName("GET /api/my/parties/simple - 내 모임 간략화 조회")
    class GetSimpleMyParties {

        @Test
        @DisplayName("200 - 사용자가 가입한 모임의 간략화된 목록을 페이징하여 반환한다")
        void success_getSimpleMyParties() throws Exception {
            mockMvc.perform(get("/api/my/parties/simple")
                            .param("page", "0")
                            .param("size", "10")
                            .param("sort", "createdAt,DESC"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("COMMON200"))
                    .andExpect(jsonPath("$.message").value("요청에 성공했습니다."))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].partyName").value("테스트 모임"))
                    .andExpect(jsonPath("$.data.content[0].partyId").value(party.getId()))
                    .andExpect(jsonPath("$.data.pageable.pageNumber").value(0))
                    .andExpect(jsonPath("$.data.last").value(true));
        }

        @Test
        @DisplayName("200 - 가입한 모임이 없을 경우 빈 목록을 반환한다")
        void success_emptySimpleMyParties() throws Exception {
            Member newMember = memberRepository.save(umc.cockple.demo.support.fixture.MemberFixture.createMember("뉴비",
                    umc.cockple.demo.global.enums.Gender.MALE, umc.cockple.demo.global.enums.Level.BEGINNER, 3003L));
            SecurityContextHelper.setAuthentication(newMember.getId(), newMember.getNickname());

            mockMvc.perform(get("/api/my/parties/simple")
                            .param("page", "0")
                            .param("size", "10")
                            .param("sort", "createdAt,DESC"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("COMMON200"))
                    .andExpect(jsonPath("$.message").value("요청에 성공했습니다."))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content").isEmpty())
                    .andExpect(jsonPath("$.data.empty").value(true));
        }
    }

    @Nested
    @DisplayName("GET /api/my/parties/suggestions - 모임 추천 조회")
    class GetRecommendedParties {

        @Test
        @DisplayName("200 - Cockple 추천 모드 시 추천된 모임 목록을 반환한다")
        void success_cockpleRecommend() throws Exception {
            mockMvc.perform(get("/api/my/parties/suggestions")
                            .param("isCockpleRecommend", "true")
                            .param("sort", "최신순")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("COMMON200"))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content[0].partyName").value("추천 모임"));
        }

        @Test
        @DisplayName("200 - 필터 모드 시 조건에 맞는 모임 목록을 반환한다")
        void success_filterMode() throws Exception {
            mockMvc.perform(get("/api/my/parties/suggestions")
                            .param("isCockpleRecommend", "false")
                            .param("addr1", "서울특별시")
                            .param("addr2", "강남구")
                            .param("sort", "최신순")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("COMMON200"))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content[0].addr1").value("서울특별시"))
                    .andExpect(jsonPath("$.data.content[0].addr2").value("강남구"));
        }

        @Test
        @DisplayName("200 - 검색 모드 시 모임명으로 검색된 결과를 반환한다")
        void success_searchMode() throws Exception {
            mockMvc.perform(get("/api/my/parties/suggestions")
                            .param("search", "추천")
                            .param("isCockpleRecommend", "false")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("COMMON200"))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content[0].partyName", containsString("추천")));
        }

        @Test
        @DisplayName("400 - 유효하지 않은 정렬 기준 입력 시 INVALID_ORDER_TYPE 에러를 반환한다")
        void fail_invalidOrderType() throws Exception {
            mockMvc.perform(get("/api/my/parties/suggestions")
                            .param("isCockpleRecommend", "false")
                            .param("sort", "잘못된순"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(PartyErrorCode.INVALID_ORDER_TYPE.getCode()));
        }

        @Test
        @DisplayName("400 - isCockpleRecommend에 부적절한 타입 입력 시 400 에러를 반환한다")
        void fail_invalidBooleanType() throws Exception {
            mockMvc.perform(get("/api/my/parties/suggestions")
                            .param("isCockpleRecommend", "not-boolean"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/parties/{partyId} - 모임 상세 조회")
    class GetPartyDetails {

        @Test
        @DisplayName("200 - 모임 상세 정보를 정상적으로 조회한다 (비회원 상태)")
        void success_getDetails_nonMember() throws Exception {
            // 모임에 가입하지 않은 새로운 유저 생성 및 인증 설정
            Member nonMember = memberRepository.save(MemberFixture.createMember("비회원", Gender.MALE, Level.C, 2001L));
            SecurityContextHelper.setAuthentication(nonMember.getId(), nonMember.getNickname());

            mockMvc.perform(get("/api/parties/{partyId}", party.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("COMMON200"))
                    .andExpect(jsonPath("$.data.partyId").value(party.getId()))
                    .andExpect(jsonPath("$.data.memberStatus").value("NOT_MEMBER"))
                    .andExpect(jsonPath("$.data.hasPendingJoinRequest").value(false));
        }

        @Test
        @DisplayName("200 - 모임원인 경우 memberStatus가 MEMBER로 반환된다")
        void success_getDetails_member() throws Exception {
            // manager는 setUp에서 이미 party의 멤버로 설정됨
            SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

            mockMvc.perform(get("/api/parties/{partyId}", party.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.memberStatus").value("MEMBER"))
                    .andExpect(jsonPath("$.data.memberRole").value("party_MANAGER"));
        }

        @Test
        @DisplayName("404 - 존재하지 않는 모임 조회 시 PARTY_NOT_FOUND 에러를 반환한다")
        void fail_partyNotFound() throws Exception {
            mockMvc.perform(get("/api/parties/{partyId}", 9999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(PartyErrorCode.PARTY_NOT_FOUND.getCode()));
        }

        @Test
        @DisplayName("400 - 삭제된 모임 조회 시 PARTY_IS_DELETED 에러를 반환한다")
        void fail_partyDeleted() throws Exception {
            // 모임 삭제 (비활성화)
            party.delete();
            partyRepository.save(party);

            mockMvc.perform(get("/api/parties/{partyId}", party.getId()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(PartyErrorCode.PARTY_IS_DELETED.getCode()));
        }
    }

    @Nested
    @DisplayName("POST /api/parties/{partyId}/join-requests - 모임 가입 신청")
    class CreateJoinRequest {

        @Test
        @DisplayName("200 - 가입하지 않은 회원이 모임 가입을 신청한다")
        void success_createJoinRequest() throws Exception {
            // 가입하지 않은 멤버
            Member applicant = memberRepository.save(MemberFixture.createMember("신청자", Gender.MALE, Level.A, 5001L, LocalDate.of(1995, 1, 1)));
            SecurityContextHelper.setAuthentication(applicant.getId(), applicant.getNickname());

            mockMvc.perform(post("/api/parties/{partyId}/join-requests", party.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("COMMON201"));
        }

        @Test
        @DisplayName("409 - 이미 가입된 회원이 다시 가입 신청을 한다")
        void fail_createJoinRequest_alreadyMember() throws Exception {
            // 이미 가입된 normalMember 사용
            SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

            mockMvc.perform(post("/api/parties/{partyId}/join-requests", party.getId()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(PartyErrorCode.ALREADY_MEMBER.getCode()));
        }

        @Test
        @DisplayName("400 - 성별 조건이 맞지 않는 모임에 신청한다")
        void fail_createJoinRequest_genderMismatch() throws Exception {
            // 여복 모임 생성
            PartyAddr addr = partyAddrRepository.save(PartyFixture.createPartyAddr("서울", "강남"));
            Party womenParty = partyRepository.save(Party.builder()
                    .partyName("여복 전용 모임")
                    .partyType(ParticipationType.WOMEN_DOUBLES)
                    .status(umc.cockple.demo.domain.party.enums.PartyStatus.ACTIVE)
                    .ownerId(manager.getId())
                    .partyAddr(addr)
                    .minBirthYear(1900)
                    .maxBirthYear(2099)
                    .activityTime(ActivityTime.MORNING)
                    .designatedCock("테스트콕")
                    .exerciseCount(0)
                    .price(0)
                    .joinPrice(0)
                    .build());

            // 남성 사용자로 신청 시도
            SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

            mockMvc.perform(post("/api/parties/{partyId}/join-requests", womenParty.getId()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(PartyErrorCode.GENDER_NOT_MATCH.getCode()));
        }
    }
}
