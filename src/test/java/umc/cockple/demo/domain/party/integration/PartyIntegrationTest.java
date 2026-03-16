package umc.cockple.demo.domain.party.integration;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.repository.MemberExerciseRepository;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.domain.PartyAddr;
import umc.cockple.demo.domain.party.exception.PartyErrorCode;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    private Member manager;
    private Member normalMember;
    private Party party;

    @BeforeEach
    void setUp() {
        manager = memberRepository.save(MemberFixture.createMember("매니저", Gender.MALE, Level.A, 1001L));
        normalMember = memberRepository.save(MemberFixture.createMember("일반멤버", Gender.FEMALE, Level.B, 1002L));

        PartyAddr addr = partyAddrRepository.save(PartyFixture.createPartyAddr("서울특별시", "강남구"));
        party = partyRepository.save(PartyFixture.createParty("테스트 모임", manager.getId(), addr));

        memberPartyRepository.save(MemberFixture.createMemberParty(party, manager, Role.party_MANAGER));
        memberPartyRepository.save(MemberFixture.createMemberParty(party, normalMember, Role.party_MEMBER));

        SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());
    }

    @AfterEach
    void tearDown() {
        memberExerciseRepository.deleteAll();
        exerciseRepository.deleteAll();
        memberPartyRepository.deleteAll();
        partyRepository.deleteAll();
        partyAddrRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Nested
    @DisplayName("GET /api/parties/{partyId}/members - 모임 멤버 조회")
    class GetPartyMembers {

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
                    .andExpect(jsonPath("$.code").value(PartyErrorCode.PARTY_NOT_FOUND.getCode()))
                    .andExpect(jsonPath("$.message").value(PartyErrorCode.PARTY_NOT_FOUND.getMessage()));
        }

        @Test
        @DisplayName("400 - 비활성화된 파티면 에러를 반환한다")
        void fail_partyInactive() throws Exception {
            party.delete();
            partyRepository.save(party);

            mockMvc.perform(get("/api/parties/{partyId}/members", party.getId()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(PartyErrorCode.PARTY_IS_DELETED.getCode()))
                    .andExpect(jsonPath("$.message").value(PartyErrorCode.PARTY_IS_DELETED.getMessage()));
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
}
