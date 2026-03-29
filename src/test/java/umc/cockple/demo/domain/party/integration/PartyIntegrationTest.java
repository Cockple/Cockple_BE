package umc.cockple.demo.domain.party.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.chat.domain.ChatRoom;
import umc.cockple.demo.domain.chat.domain.ChatRoomMember;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomRepository;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberAddr;
import umc.cockple.demo.domain.member.domain.MemberParty;
import umc.cockple.demo.domain.member.repository.MemberAddrRepository;
import umc.cockple.demo.domain.member.repository.MemberExerciseRepository;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.domain.PartyAddr;
import umc.cockple.demo.domain.party.domain.PartyInvitation;
import umc.cockple.demo.domain.party.domain.PartyJoinRequest;
import umc.cockple.demo.domain.party.dto.PartyCreateDTO;
import umc.cockple.demo.domain.party.dto.PartyInviteCreateDTO;
import umc.cockple.demo.domain.party.dto.PartyInviteActionDTO;
import umc.cockple.demo.domain.party.dto.PartyJoinActionDTO;
import umc.cockple.demo.domain.party.dto.PartyKeywordDTO;
import umc.cockple.demo.domain.party.dto.PartyMemberRoleDTO;
import umc.cockple.demo.domain.party.dto.PartyUpdateDTO;
import umc.cockple.demo.domain.party.enums.ActivityTime;
import umc.cockple.demo.domain.party.enums.ParticipationType;
import umc.cockple.demo.domain.party.enums.RequestAction;
import umc.cockple.demo.domain.party.enums.RequestStatus;
import umc.cockple.demo.domain.party.exception.PartyErrorCode;
import umc.cockple.demo.domain.party.repository.PartyAddrRepository;
import umc.cockple.demo.domain.party.repository.PartyInvitationRepository;
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
import java.util.List;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
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
    @Autowired
    PartyInvitationRepository partyInvitationRepository;
    @Autowired
    ObjectMapper objectMapper;

    private Member manager;
    private Member normalMember;
    private Party party;

    @BeforeEach
    void setUp() {
        // 매니저 및 주소 정보 생성
        manager = memberRepository.save(MemberFixture.createMember("매니저", Gender.MALE, Level.A, 1001L, LocalDate.of(1995, 1, 1)));
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

        // 일반 멤버 생성
        normalMember = memberRepository.save(MemberFixture.createMember("일반멤버", Gender.FEMALE, Level.B, 1002L));

        // 모임 및 주소 정보 생성
        PartyAddr addr = partyAddrRepository.save(PartyFixture.createPartyAddr("서울특별시", "강남구"));
        party = partyRepository.save(PartyFixture.createParty("테스트 모임", manager.getId(), addr));

        // 모임 멤버 생성
        memberPartyRepository.save(MemberFixture.createMemberParty(party, manager, Role.party_MANAGER));
        memberPartyRepository.save(MemberFixture.createMemberParty(party, normalMember, Role.party_MEMBER));

        // 채팅방 생성
        ChatRoom chatRoom = chatRoomRepository.save(ChatRoom.createPartyChatRoom(party));
        chatRoomMemberRepository.save(ChatRoomMember.create(chatRoom, manager));
        chatRoomMemberRepository.save(ChatRoomMember.create(chatRoom, normalMember));

        // 추천 조회용 모임 (manager의 조건에 맞춤)
        Party suggestedParty = PartyFixture.createParty("추천 모임", normalMember.getId(), addr);
        suggestedParty.addLevel(Gender.MALE, Level.A); 
        partyRepository.save(suggestedParty);

        SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());
    }


    @Nested
    @DisplayName("GET /api/parties/{partyId}/members - 모임 멤버 조회")
    class GetPartyMembers {

        @Test
        @DisplayName("200 - 멤버 목록을 역할, 성별 통계 및 마지막 운동일과 함께 조회한다")
        void success_getPartyMembers() throws Exception {
            // 부모임장 추가
            Member subManager = memberRepository.save(MemberFixture.createMember("부매니저", Gender.MALE, Level.A, 1003L));
            memberPartyRepository.save(MemberFixture.createMemberParty(party, subManager, Role.party_SUBMANAGER));

            // 운동 기록 추가
            Exercise exercise = exerciseRepository.save(ExerciseFixture.createExercise(party, LocalDate.of(2025, 1, 10)));
            memberExerciseRepository.save(MemberFixture.createMemberExercise(normalMember, exercise));

            mockMvc.perform(get("/api/parties/{partyId}/members", party.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.summary.totalCount").value(3))
                    .andExpect(jsonPath("$.data.summary.maleCount").value(2))
                    .andExpect(jsonPath("$.data.summary.femaleCount").value(1))
                    .andExpect(jsonPath("$.data.members[0].role").value("party_MANAGER"))
                    .andExpect(jsonPath("$.data.members[0].isMe").value(true))
                    .andExpect(jsonPath("$.data.members[1].role").value("party_SUBMANAGER"))
                    .andExpect(jsonPath("$.data.members[2].role").value("party_MEMBER"))
                    .andExpect(jsonPath("$.data.members[2].lastExerciseDate").value("2025-01-10"));
        }

        @Test
        @DisplayName("404 - 존재하지 않는 파티면 에러를 반환한다")
        void fail_getPartyMembers_partyNotFound() throws Exception {
            mockMvc.perform(get("/api/parties/{partyId}/members", 999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(PartyErrorCode.PARTY_NOT_FOUND.getCode()));
        }

        @Test
        @DisplayName("400 - 비활성화된 파티면 에러를 반환한다")
        void fail_getPartyMembers_partyInactive() throws Exception {
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
        void success_getRecommendedParties_cockpleRecommend() throws Exception {
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
        void success_getRecommendedParties_filterMode() throws Exception {
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
        void success_getRecommendedParties_searchMode() throws Exception {
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
        void fail_getRecommendedParties_invalidOrderType() throws Exception {
            mockMvc.perform(get("/api/my/parties/suggestions")
                            .param("isCockpleRecommend", "false")
                            .param("sort", "잘못된순"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(PartyErrorCode.INVALID_ORDER_TYPE.getCode()));
        }

        @Test
        @DisplayName("400 - isCockpleRecommend에 부적절한 타입 입력 시 400 에러를 반환한다")
        void fail_getRecommendedParties_invalidBooleanType() throws Exception {
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
        void success_getPartyDetails_nonMember() throws Exception {
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
        void success_getPartyDetails_member() throws Exception {
            // manager는 setUp에서 이미 party의 멤버로 설정됨
            SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

            mockMvc.perform(get("/api/parties/{partyId}", party.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.memberStatus").value("MEMBER"))
                    .andExpect(jsonPath("$.data.memberRole").value("party_MANAGER"));
        }

        @Test
        @DisplayName("404 - 존재하지 않는 모임 조회 시 PARTY_NOT_FOUND 에러를 반환한다")
        void fail_getPartyDetails_partyNotFound() throws Exception {
            mockMvc.perform(get("/api/parties/{partyId}", 9999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(PartyErrorCode.PARTY_NOT_FOUND.getCode()));
        }

        @Test
        @DisplayName("400 - 삭제된 모임 조회 시 PARTY_IS_DELETED 에러를 반환한다")
        void fail_getPartyDetails_partyDeleted() throws Exception {
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
            // 가입하지 않은 멤버 생성
            Member applicant = memberRepository.save(MemberFixture.createMember("신청자", Gender.MALE, Level.A, 5001L, LocalDate.of(1995, 1, 1)));
            SecurityContextHelper.setAuthentication(applicant.getId(), applicant.getNickname());

            mockMvc.perform(post("/api/parties/{partyId}/join-requests", party.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("COMMON201"));

            // 가입 신청 데이터 확인
            boolean exists = partyJoinRequestRepository.existsByPartyAndMemberAndStatus(party, applicant, RequestStatus.PENDING);
            assertThat(exists).isTrue();
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

            SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

            mockMvc.perform(post("/api/parties/{partyId}/join-requests", womenParty.getId()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(PartyErrorCode.GENDER_NOT_MATCH.getCode()));
        }
    }

    @Nested
    @DisplayName("PATCH /api/parties/{partyId} - 모임 정보 수정")
    class UpdateParty {

        @Test
        @DisplayName("200 - 모임장이 유효한 데이터로 모임 정보를 정상적으로 수정한다")
        void success_updateParty() throws Exception {
            // given
            PartyUpdateDTO.Request request = PartyUpdateDTO.Request.builder()
                    .activityDay(List.of("월", "수"))
                    .activityTime("오전")
                    .designatedCock("수정된 콕")
                    .joinPrice(2000)
                    .price(15000)
                    .content("수정된 내용입니다.")
                    .build();

            SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

            // when & then
            mockMvc.perform(patch("/api/parties/{partyId}", party.getId())
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("COMMON200"));

            // 검증
            Party updatedParty = partyRepository.findById(party.getId()).orElseThrow();
            assertThat(updatedParty.getDesignatedCock()).isEqualTo("수정된 콕");
            assertThat(updatedParty.getJoinPrice()).isEqualTo(2000);
            assertThat(updatedParty.getPrice()).isEqualTo(15000);
            assertThat(updatedParty.getContent()).isEqualTo("수정된 내용입니다.");
        }

        @Test
        @DisplayName("400 - 필수 필드(activityDay, activityTime) 누락 시 에러를 반환한다")
        void fail_updateParty_missingRequiredFields() throws Exception {
            // given
            PartyUpdateDTO.Request request = PartyUpdateDTO.Request.builder()
                    .activityDay(null)
                    .activityTime("")
                    .build();

            SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

            // when & then
            mockMvc.perform(patch("/api/parties/{partyId}", party.getId())
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMON400_VALIDATION"));
        }

        @Test
        @DisplayName("403 - 모임장이 아닌 일반 멤버가 수정을 시도하면 INSUFFICIENT_PERMISSION 에러를 반환한다")
        void fail_updateParty_notOwner() throws Exception {
            // given
            PartyUpdateDTO.Request request = PartyUpdateDTO.Request.builder()
                    .activityDay(List.of("토", "일"))
                    .activityTime("오후")
                    .build();

            // 일반 멤버로 세션 설정
            SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

            // when & then
            mockMvc.perform(patch("/api/parties/{partyId}", party.getId())
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(PartyErrorCode.INSUFFICIENT_PERMISSION.getCode()));
        }
    }

    @Nested
    @DisplayName("PATCH /api/parties/{partyId}/status - 모임 삭제")
    class DeleteParty {

        @Test
        @DisplayName("200 - 모임장이 모임을 성공적으로 삭제(비활성화)한다")
        void success_deleteParty() throws Exception {
            // given
            SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

            // when & then
            mockMvc.perform(patch("/api/parties/{partyId}/status", party.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("COMMON200"));

            // 검증
            Party deletedParty = partyRepository.findById(party.getId()).orElseThrow();
            assertThat(deletedParty.getStatus()).isEqualTo(umc.cockple.demo.domain.party.enums.PartyStatus.INACTIVE);
        }

        @Test
        @DisplayName("403 - 모임장이 아닌 멤버가 삭제를 시도하면 INSUFFICIENT_PERMISSION 예외를 반환한다")
        void fail_deleteParty_notOwner() throws Exception {
            // given
            SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

            // when & then
            mockMvc.perform(patch("/api/parties/{partyId}/status", party.getId()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(PartyErrorCode.INSUFFICIENT_PERMISSION.getCode()));
        }

        @Test
        @DisplayName("400 - 이미 삭제된 모임을 다시 삭제 시도하면 PARTY_IS_DELETED 예외를 반환한다")
        void fail_deleteParty_partyDeleted() throws Exception {
            // given
            party.delete(); // 상태 INACTIVE 변경
            partyRepository.save(party);

            SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

            // when & then
            mockMvc.perform(patch("/api/parties/{partyId}/status", party.getId()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(PartyErrorCode.PARTY_IS_DELETED.getCode()));
        }
    }

    @Nested
    @DisplayName("DELETE /api/parties/{partyId}/members/{memberId} - 모임 멤버 삭제")
    class RemoveMember {

        @Test
        @DisplayName("200 - 모임장이 일반 멤버를 성공적으로 강퇴한다")
        void success_removeMember() throws Exception {
            // given
            SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

            // when & then
            mockMvc.perform(delete("/api/parties/{partyId}/members/{memberId}", party.getId(), normalMember.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("COMMON200"));

            // 검증
            boolean exists = memberPartyRepository.existsByPartyAndMember(party, normalMember);
            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("403 - 모임장이 아닌 멤버가 삭제를 시도하면 INSUFFICIENT_PERMISSION 에러를 반환한다")
        void fail_removeMember_notOwner() throws Exception {
            // given
            Member someoneElse = memberRepository.save(MemberFixture.createMember("다른멤버", Gender.MALE, Level.B, 1010L));
            memberPartyRepository.save(MemberFixture.createMemberParty(party, someoneElse, Role.party_MEMBER));
            SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

            // when & then
            mockMvc.perform(delete("/api/parties/{partyId}/members/{memberId}", party.getId(), someoneElse.getId()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(PartyErrorCode.INSUFFICIENT_PERMISSION.getCode()));
        }

        @Test
        @DisplayName("400 - 모임장이 자기 자신을 강퇴하려 할 경우 CANNOT_REMOVE_SELF 에러를 반환한다")
        void fail_removeMember_selfAsManager() throws Exception {
            // given
            SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

            // when & then
            mockMvc.perform(delete("/api/parties/{partyId}/members/{memberId}", party.getId(), manager.getId()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(PartyErrorCode.CANNOT_REMOVE_SELF.getCode()));
        }
    }

    @Nested
    @DisplayName("GET /api/parties/{partyId}/join-requests - 모임 가입 신청 조회")
    class GetJoinRequests {

        @Test
        @DisplayName("200 - 모임장이 가입 신청 목록을 정상적으로 조회한다")
        void success_getJoinRequests() throws Exception {
            // given
            Member applicant = memberRepository.save(MemberFixture.createMember("가입희망자", Gender.FEMALE, Level.B, 1010L));
            
            PartyJoinRequest joinRequest = PartyJoinRequest.builder()
                    .party(party)
                    .member(applicant)
                    .status(RequestStatus.PENDING)
                    .build();
            partyJoinRequestRepository.save(joinRequest);

            SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

            // when & then
            mockMvc.perform(get("/api/parties/{partyId}/join-requests", party.getId())
                            .param("status", "PENDING")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("COMMON200"))
                    .andExpect(jsonPath("$.data.content[0].userId").value(applicant.getId()));
        }

        @Test
        @DisplayName("200 - 모임장이 가입 승인된 멤버 목록(APPROVED)을 정상적으로 조회한다")
        void success_getJoinRequests_approved() throws Exception {
            // given
            Member applicant = memberRepository.save(MemberFixture.createMember("승인된멤버", Gender.MALE, Level.C, 1015L));
            
            PartyJoinRequest joinRequest = PartyJoinRequest.builder()
                    .party(party)
                    .member(applicant)
                    .status(RequestStatus.APPROVED)
                    .build();
            partyJoinRequestRepository.save(joinRequest);

            SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

            // when & then
            mockMvc.perform(get("/api/parties/{partyId}/join-requests", party.getId())
                            .param("status", "APPROVED")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("COMMON200"))
                    .andExpect(jsonPath("$.data.content[0].userId").value(applicant.getId()));
        }

        @Test
        @DisplayName("403 - 모임장이 아닌 사용자가 조회하면 INSUFFICIENT_PERMISSION 예외가 반환된다")
        void fail_getJoinRequests_notOwner() throws Exception {
            // given
            SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

            // when & then
            mockMvc.perform(get("/api/parties/{partyId}/join-requests", party.getId())
                            .param("status", "PENDING"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(PartyErrorCode.INSUFFICIENT_PERMISSION.getCode()));
        }

        @Test
        @DisplayName("400 - 잘못된 상태값을 전달하면 INVALID_REQUEST_STATUS 예외가 반환된다")
        void fail_getJoinRequests_invalidStatus() throws Exception {
            // given
            SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

            // when & then
            mockMvc.perform(get("/api/parties/{partyId}/join-requests", party.getId())
                            .param("status", "UNKNOWN_STATUS"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(PartyErrorCode.INVALID_REQUEST_STATUS.getCode()));
        }
    }

    @Nested
    @DisplayName("GET /api/parties/{partyId}/members/suggestions - 신규 멤버 추천받기")
    class GetRecommendedMembers {

        @Test
        @DisplayName("200 - 추천 조건(지역/나이/급수)에 맞는 멤버가 추천 목록에 포함된다")
        void success_getRecommendedMembers() throws Exception {
            // given
            // party의 추천 조건: addr1=서울특별시, minBirthYear=1990, maxBirthYear=2005
            // party에 남성 A급 레벨 추가
            party.addLevel(Gender.MALE, Level.A);
            partyRepository.save(party);

            // 추천 조건을 모두 만족하는 멤버: 남성, A급, 생년 1995, 서울특별시 주소(isMain=true)
            Member suggestedMember = memberRepository.save(
                    MemberFixture.createMember("추천회원", Gender.MALE, Level.A, 1080L, LocalDate.of(1995, 6, 1))
            );
            memberAddrRepository.save(MemberAddr.builder()
                    .member(suggestedMember)
                    .addr1("서울특별시")
                    .addr2("강남구")
                    .addr3("역삼동")
                    .streetAddr("테헤란로")
                    .latitude(37.5)
                    .longitude(127.0)
                    .isMain(true)
                    .build());

            SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

            // when & then
            mockMvc.perform(get("/api/parties/{partyId}/members/suggestions", party.getId())
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("COMMON200"))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content[0].userId").value(suggestedMember.getId()));
        }

        @Test
        @DisplayName("404 - 존재하지 않는 모임의 추천 멤버를 조회하면 PARTY_NOT_FOUND 예외 발생")
        void fail_getRecommendedMembers_partyNotFound() throws Exception {
            // given
            Long invalidPartyId = 9999L;
            SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

            // when & then
            mockMvc.perform(get("/api/parties/{partyId}/members/suggestions", invalidPartyId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(PartyErrorCode.PARTY_NOT_FOUND.getCode()));
        }
    }

    @Nested
    @DisplayName("POST /api/parties/{partyId}/invitations - 신규 멤버 초대 보내기")
    class CreateInvitation {

        @Test
        @DisplayName("200 - 모임장이 새로운 멤버를 초대하고 invitationId를 반환한다")
        void success_createInvitation() throws Exception {
            // given
            Member newMember = memberRepository.save(MemberFixture.createMember("새멤버", Gender.FEMALE, Level.B, 1090L));
            PartyInviteCreateDTO.Request request = new PartyInviteCreateDTO.Request(newMember.getId());
            SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

            // when & then
            mockMvc.perform(post("/api/parties/{partyId}/invitations", party.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("COMMON201"))
                    .andExpect(jsonPath("$.data.invitationId").exists());
        }

        @Test
        @DisplayName("403 - 모임장이 아닌 사용자가 초대하면 INSUFFICIENT_PERMISSION 발생")
        void fail_createInvitation_notOwner() throws Exception {
            // given
            Member newMember = memberRepository.save(MemberFixture.createMember("새멤버", Gender.FEMALE, Level.B, 1091L));
            PartyInviteCreateDTO.Request request = new PartyInviteCreateDTO.Request(newMember.getId());
            SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

            // when & then
            mockMvc.perform(post("/api/parties/{partyId}/invitations", party.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(PartyErrorCode.INSUFFICIENT_PERMISSION.getCode()));
        }

        @Test
        @DisplayName("409 - 이미 모임 멤버인 사람을 초대하면 ALREADY_MEMBER 발생")
        void fail_createInvitation_alreadyMember() throws Exception {
            // given - normalMember는 setUp()에서 이미 모임 멤버로 추가된 상태
            PartyInviteCreateDTO.Request request = new PartyInviteCreateDTO.Request(normalMember.getId());
            SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

            // when & then
            mockMvc.perform(post("/api/parties/{partyId}/invitations", party.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(PartyErrorCode.ALREADY_MEMBER.getCode()));
        }

        @Test
        @DisplayName("409 - 이미 대기 중인 초대가 있는 멤버를 중복 초대하면 INVITATION_ALREADY_EXISTS 발생")
        void fail_createInvitation_duplicateInvitation() throws Exception {
            // given
            Member newMember = memberRepository.save(MemberFixture.createMember("새멤버", Gender.FEMALE, Level.B, 1092L));
            partyInvitationRepository.save(PartyInvitation.create(party, manager, newMember));

            PartyInviteCreateDTO.Request request = new PartyInviteCreateDTO.Request(newMember.getId());
            SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

            // when & then
            mockMvc.perform(post("/api/parties/{partyId}/invitations", party.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(PartyErrorCode.INVITATION_ALREADY_EXISTS.getCode()));
        }
    }

    @Nested
    @DisplayName("PATCH /api/parties/invitations/{invitationId} - 모임 초대 처리")
    class ActionInvitation {

        @Test
        @DisplayName("200 - 초대받은 멤버가 승인하면 모임 멤버로 추가된다")
        void success_actionInvitation_approve() throws Exception {
            // given
            Member invitee = memberRepository.save(MemberFixture.createMember("초대대상", Gender.FEMALE, Level.B, 1100L));

            PartyInvitation invitation = partyInvitationRepository.save(
                    PartyInvitation.create(party, manager, invitee)
            );

            PartyInviteActionDTO.Request request = new PartyInviteActionDTO.Request(RequestAction.APPROVE);
            SecurityContextHelper.setAuthentication(invitee.getId(), invitee.getNickname());

            // when & then
            mockMvc.perform(patch("/api/parties/invitations/{invitationId}", invitation.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("COMMON200"));

            // 검증
            PartyInvitation updated = partyInvitationRepository.findById(invitation.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(RequestStatus.APPROVED);
            assertThat(memberPartyRepository.existsByPartyAndMember(party, invitee)).isTrue();
        }

        @Test
        @DisplayName("200 - 초대받은 멤버가 거절하면 상태가 REJECTED로 바뀌고 멤버로 추가되지 않는다")
        void success_actionInvitation_reject() throws Exception {
            // given
            Member invitee = memberRepository.save(MemberFixture.createMember("초대대상", Gender.FEMALE, Level.B, 1101L));

            PartyInvitation invitation = partyInvitationRepository.save(
                    PartyInvitation.create(party, manager, invitee)
            );

            PartyInviteActionDTO.Request request = new PartyInviteActionDTO.Request(RequestAction.REJECT);
            SecurityContextHelper.setAuthentication(invitee.getId(), invitee.getNickname());

            // when & then
            mockMvc.perform(patch("/api/parties/invitations/{invitationId}", invitation.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("COMMON200"));

            // 검증
            PartyInvitation updated = partyInvitationRepository.findById(invitation.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(RequestStatus.REJECTED);
            assertThat(memberPartyRepository.existsByPartyAndMember(party, invitee)).isFalse();
        }

        @Test
        @DisplayName("403 - 초대받은 사람이 아닌 제3자가 처리하면 NOT_YOUR_INVITATION 발생")
        void fail_actionInvitation_notYourInvitation() throws Exception {
            // given
            Member invitee = memberRepository.save(MemberFixture.createMember("초대대상", Gender.FEMALE, Level.B, 1102L));

            PartyInvitation invitation = partyInvitationRepository.save(
                    PartyInvitation.create(party, manager, invitee)
            );

            PartyInviteActionDTO.Request request = new PartyInviteActionDTO.Request(RequestAction.APPROVE);
            // normalMember는 초대받은 사람이 아님
            SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

            // when & then
            mockMvc.perform(patch("/api/parties/invitations/{invitationId}", invitation.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(PartyErrorCode.NOT_YOUR_INVITATION.getCode()));
        }

        @Test
        @DisplayName("409 - 이미 처리된 초대를 다시 처리하면 INVITATION_ALREADY_ACTIONS 발생")
        void fail_actionInvitation_alreadyActions() throws Exception {
            // given
            Member invitee = memberRepository.save(MemberFixture.createMember("초대대상", Gender.FEMALE, Level.B, 1103L));

            // 이미 APPROVED 처리된 초대
            PartyInvitation invitation = partyInvitationRepository.save(
                    PartyInvitation.create(party, manager, invitee)
            );
            invitation.updateStatus(RequestStatus.APPROVED);
            partyInvitationRepository.save(invitation);

            PartyInviteActionDTO.Request request = new PartyInviteActionDTO.Request(RequestAction.REJECT);
            SecurityContextHelper.setAuthentication(invitee.getId(), invitee.getNickname());

            // when & then
            mockMvc.perform(patch("/api/parties/invitations/{invitationId}", invitation.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(PartyErrorCode.INVITATION_ALREADY_ACTIONS.getCode()));
        }
    }

    @Nested
    @DisplayName("PATCH /api/parties/{partyId}/join-requests/{requestId} - 모임 가입 신청 처리")
    class ActionJoinRequest {

        @Test
        @DisplayName("200 - 모임장이 가입 신청을 성공적으로 승인한다")
        void success_actionJoinRequest_approve() throws Exception {
            // given
            Member applicant = memberRepository.save(MemberFixture.createMember("지원자", Gender.FEMALE, Level.B, 1020L));
            
            PartyJoinRequest joinRequest = partyJoinRequestRepository.save(PartyJoinRequest.builder()
                    .party(party)
                    .member(applicant)
                    .status(RequestStatus.PENDING)
                    .build());

            PartyJoinActionDTO.Request actionRequest = new PartyJoinActionDTO.Request(RequestAction.APPROVE);
            SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

            // when & then
            mockMvc.perform(patch("/api/parties/{partyId}/join-requests/{requestId}", party.getId(), joinRequest.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(actionRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("COMMON200"));

            // 검증
            PartyJoinRequest updatedRequest = partyJoinRequestRepository.findById(joinRequest.getId()).orElseThrow();
            assertThat(updatedRequest.getStatus()).isEqualTo(RequestStatus.APPROVED);
            boolean isMember = memberPartyRepository.existsByPartyAndMember(party, applicant);
            assertThat(isMember).isTrue();
        }

        @Test
        @DisplayName("200 - 모임장이 가입 신청을 성공적으로 거절한다")
        void success_actionJoinRequest_reject() throws Exception {
            // given
            Member applicant = memberRepository.save(MemberFixture.createMember("탈락자", Gender.FEMALE, Level.B, 1030L));
            
            PartyJoinRequest joinRequest = partyJoinRequestRepository.save(PartyJoinRequest.builder()
                    .party(party)
                    .member(applicant)
                    .status(RequestStatus.PENDING)
                    .build());

            PartyJoinActionDTO.Request actionRequest = new PartyJoinActionDTO.Request(RequestAction.REJECT);
            SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

            // when & then
            mockMvc.perform(patch("/api/parties/{partyId}/join-requests/{requestId}", party.getId(), joinRequest.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(actionRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("COMMON200"));

            // 검증
            PartyJoinRequest updatedRequest = partyJoinRequestRepository.findById(joinRequest.getId()).orElseThrow();
            assertThat(updatedRequest.getStatus()).isEqualTo(RequestStatus.REJECTED);
            boolean isMember = memberPartyRepository.existsByPartyAndMember(party, applicant);
            assertThat(isMember).isFalse();
        }

        @Test
        @DisplayName("403 - 모임장이 아닌 사용자가 가입 신청을 처리하려 하면 INSUFFICIENT_PERMISSION 발생")
        void fail_actionJoinRequest_notOwner() throws Exception {
            // given
            Member applicant = memberRepository.save(MemberFixture.createMember("지원자", Gender.FEMALE, Level.B, 1040L));
            
            PartyJoinRequest joinRequest = partyJoinRequestRepository.save(PartyJoinRequest.builder()
                    .party(party)
                    .member(applicant)
                    .status(RequestStatus.PENDING)
                    .build());

            PartyJoinActionDTO.Request actionRequest = new PartyJoinActionDTO.Request(RequestAction.APPROVE);
            SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

            // when & then
            mockMvc.perform(patch("/api/parties/{partyId}/join-requests/{requestId}", party.getId(), joinRequest.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(actionRequest)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(PartyErrorCode.INSUFFICIENT_PERMISSION.getCode()));
        }

        @Test
        @DisplayName("409 - 이미 처리된 가입 신청을 다시 처리하려 할 때 JOIN_REQUEST_ALREADY_ACTIONS 상태 반환")
        void fail_actionJoinRequest_alreadyHandled() throws Exception {
            // given
            Member applicant = memberRepository.save(MemberFixture.createMember("지원자", Gender.FEMALE, Level.B, 1050L));
            
            PartyJoinRequest joinRequest = partyJoinRequestRepository.save(PartyJoinRequest.builder()
                    .party(party)
                    .member(applicant)
                    .status(RequestStatus.APPROVED)
                    .build());

            PartyJoinActionDTO.Request actionRequest = new PartyJoinActionDTO.Request(RequestAction.APPROVE);
            SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

            // when & then
            mockMvc.perform(patch("/api/parties/{partyId}/join-requests/{requestId}", party.getId(), joinRequest.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(actionRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(PartyErrorCode.JOIN_REQUEST_ALREADY_ACTIONS.getCode()));
        }
    }

    @Nested
    @DisplayName("POST /api/parties - 모임 생성")
    class CreateParty {

        @Test
        @DisplayName("200 - 모임을 성공적으로 생성하고 DB 저장 상태를 확인한다")
        void success_createParty() throws Exception {
            // given
            PartyCreateDTO.Request request = PartyCreateDTO.Request.builder()
                    .partyName("새로운 통합 모임")
                    .partyType("혼복")
                    .minBirthYear(1990)
                    .maxBirthYear(2000)
                    .activityTime("오전")
                    .addr1("서울특별시")
                    .addr2("강남구")
                    .activityDay(List.of("월", "수"))
                    .price(10000)
                    .joinPrice(5000)
                    .designatedCock("통합테스트콕")
                    .maleLevel(List.of("A조"))
                    .femaleLevel(List.of("B조"))
                    .build();

            SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

            // when & then
            mockMvc.perform(post("/api/parties")
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("COMMON201"))
                    .andExpect(jsonPath("$.data.partyId").exists());

            // 검증
            List<Party> parties = partyRepository.findAll();
            Party createdParty = parties.stream()
                    .filter(p -> p.getPartyName().equals("새로운 통합 모임"))
                    .findFirst()
                    .orElseThrow();

            assertThat(createdParty.getOwnerId()).isEqualTo(manager.getId());
            assertThat(createdParty.getDesignatedCock()).isEqualTo("통합테스트콕");
        }

        @Test
        @DisplayName("400 - 본인의 나이가 모임 조건에 맞지 않을 때 에러를 반환한다")
        void fail_createParty_invalidAgeRange() throws Exception {
            // given
            // manager는 1995년생. 모임 조건을 2000~2010으로 설정.
            PartyCreateDTO.Request request = PartyCreateDTO.Request.builder()
                    .partyName("청년 모임")
                    .partyType("혼복")
                    .minBirthYear(2000)
                    .maxBirthYear(2010)
                    .activityTime("오후")
                    .activityDay(List.of("금"))
                    .addr1("서울특별시")
                    .addr2("강남구")
                    .price(10000)
                    .joinPrice(0)
                    .femaleLevel(List.of("A조"))
                    .maleLevel(List.of("A조"))
                    .designatedCock("청년콕")
                    .build();

            SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

            // when & then
            mockMvc.perform(post("/api/parties")
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(PartyErrorCode.AGE_NOT_MATCH.getCode()));
        }

        @Test
        @DisplayName("400 - 혼복 모임에서 남자 급수 정보가 누락되었을 때 에러를 반환한다")
        void fail_createParty_missingMaleLevelInMixDoubles() throws Exception {
            // given
            PartyCreateDTO.Request request = PartyCreateDTO.Request.builder()
                    .partyName("혼복 모임")
                    .partyType("혼복")
                    .minBirthYear(1990)
                    .maxBirthYear(2005)
                    .activityTime("오전")
                    .activityDay(List.of("토"))
                    .addr1("서울특별시")
                    .addr2("강남구")
                    .price(10000)
                    .joinPrice(0)
                    .designatedCock("혼복콕")
                    .maleLevel(null)
                    .femaleLevel(List.of("A조"))
                    .build();

            SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

            // when & then
            mockMvc.perform(post("/api/parties")
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(PartyErrorCode.MALE_LEVEL_REQUIRED.getCode()));
        }
    }

    @Nested
    @DisplayName("PATCH /api/parties/{partyId}/members/{memberId}/role - 멤버 역할(부모임장) 설정")
    class UpdateMemberRole {

        @Test
        @DisplayName("200 - 모임장이 일반 멤버를 부모임장으로 성공적으로 임명한다")
        void success_updateMemberRole() throws Exception {
            // given
            PartyMemberRoleDTO.Request request = new PartyMemberRoleDTO.Request(Role.party_SUBMANAGER);
            SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

            // when & then
            mockMvc.perform(patch("/api/parties/{partyId}/members/{memberId}/role", party.getId(), normalMember.getId())
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("COMMON200"));

            // 검증
            MemberParty targetMemberParty = memberPartyRepository.findByPartyAndMember(party, normalMember).orElseThrow();
            assertThat(targetMemberParty.getRole()).isEqualTo(Role.party_SUBMANAGER);
        }

        @Test
        @DisplayName("403 - 모임장이 아닌 멤버가 역할 수정을 시도하면 INSUFFICIENT_PERMISSION 예외를 반환한다")
        void fail_updateMemberRole_notOwner() throws Exception {
            // given
            PartyMemberRoleDTO.Request request = new PartyMemberRoleDTO.Request(Role.party_SUBMANAGER);
            // 일반 멤버가 권한 변경 시도
            SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

            // when & then
            mockMvc.perform(patch("/api/parties/{partyId}/members/{memberId}/role", party.getId(), normalMember.getId())
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(PartyErrorCode.INSUFFICIENT_PERMISSION.getCode()));
        }

        @Test
        @DisplayName("403 - 대상자가 모임장인 경우 권한 변경은 실패하며 CANNOT_ASSIGN_TO_OWNER 예외를 반환한다")
        void fail_updateMemberRole_targetIsOwner() throws Exception {
            // given
            PartyMemberRoleDTO.Request request = new PartyMemberRoleDTO.Request(Role.party_MEMBER);
            SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

            // when & then
            mockMvc.perform(patch("/api/parties/{partyId}/members/{memberId}/role", party.getId(), manager.getId())
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(PartyErrorCode.CANNOT_ASSIGN_TO_OWNER.getCode()));
        }
    }

    @Nested
    @DisplayName("POST /api/parties/{partyId}/keywords - 키워드 추가")
    class AddKeyword {

        @Test
        @DisplayName("200 - 모임장이 유효한 키워드를 정상적으로 추가한다")
        void success_addKeyword() throws Exception {
            // given
            PartyKeywordDTO.Request request = new PartyKeywordDTO.Request(
                    List.of("친목", "가입비 무료")
            );
            SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

            // when & then
            mockMvc.perform(post("/api/parties/{partyId}/keywords", party.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("COMMON200"));

            // 검증 - DB에 키워드가 실제로 저장됐는지 확인
            Party updatedParty = partyRepository.findById(party.getId()).orElseThrow();
            assertThat(updatedParty.getKeywords()).hasSize(2);
        }

        @Test
        @DisplayName("403 - 모임장이 아닌 사용자가 키워드를 추가하면 INSUFFICIENT_PERMISSION 발생")
        void fail_addKeyword_notOwner() throws Exception {
            // given
            PartyKeywordDTO.Request request = new PartyKeywordDTO.Request(List.of("친목"));
            SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

            // when & then
            mockMvc.perform(post("/api/parties/{partyId}/keywords", party.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(PartyErrorCode.INSUFFICIENT_PERMISSION.getCode()));
        }

        @Test
        @DisplayName("400 - 유효하지 않은 키워드 문자열을 전달하면 INVALID_KEYWORD 발생")
        void fail_addKeyword_invalidKeyword() throws Exception {
            // given
            PartyKeywordDTO.Request request = new PartyKeywordDTO.Request(List.of("존재하지않는키워드"));
            SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

            // when & then
            mockMvc.perform(post("/api/parties/{partyId}/keywords", party.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(PartyErrorCode.INVALID_KEYWORD.getCode()));
        }
    }
}
