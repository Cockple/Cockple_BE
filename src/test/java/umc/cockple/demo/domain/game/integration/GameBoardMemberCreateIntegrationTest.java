package umc.cockple.demo.domain.game.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.game.domain.GameBoardMember;
import umc.cockple.demo.domain.game.exception.GameErrorCode;
import umc.cockple.demo.domain.game.repository.GameBoardMemberRepository;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.domain.PartyAddr;
import umc.cockple.demo.domain.party.repository.PartyAddrRepository;
import umc.cockple.demo.domain.party.repository.PartyRepository;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.IntegrationTestBase;
import umc.cockple.demo.support.SecurityContextHelper;
import umc.cockple.demo.support.fixture.ExerciseFixture;
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@DisplayName("게임판 명단 추가 통합 테스트")
class GameBoardMemberCreateIntegrationTest extends IntegrationTestBase {

    @Autowired private MockMvc mockMvc;
    @Autowired private MemberRepository memberRepository;
    @Autowired private PartyAddrRepository partyAddrRepository;
    @Autowired private PartyRepository partyRepository;
    @Autowired private ExerciseRepository exerciseRepository;
    @Autowired private GameBoardMemberRepository gameBoardMemberRepository;

    private Member gameHost;
    private Member otherMember;
    private Exercise exercise;

    @BeforeEach
    void setUp() {
        gameHost = memberRepository.save(MemberFixture.createMemberWithName(
                "게임 진행자", "진행자", Gender.FEMALE, Level.A, 72001L));
        otherMember = memberRepository.save(MemberFixture.createMemberWithName(
                "일반 회원", "일반", Gender.MALE, Level.B, 72002L));

        PartyAddr partyAddr = partyAddrRepository.save(
                PartyFixture.createPartyAddr("서울특별시", "강남구"));
        Party party = partyRepository.save(
                PartyFixture.createParty("명단 추가 테스트 모임", gameHost.getId(), partyAddr));
        exercise = exerciseRepository.save(
                ExerciseFixture.createExerciseWithAddr(party, LocalDate.of(2099, 12, 31)));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHelper.clearAuthentication();
    }

    @Test
    @DisplayName("게임 진행자가 한글 입력으로 수동 명단을 생성한다")
    void createMember_createsManualMemberWithDefaults() throws Exception {
        authenticate(gameHost);
        String request = """
                {
                  "name": "  김세익  ",
                  "gender": "남성",
                  "level": "D조",
                  "ageGroup": "30대"
                }
                """;

        mockMvc.perform(post("/api/game-boards/{gameBoardId}/gameBoardMembers", exercise.getGameBoard().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.gameBoardMemberId").isNumber());

        List<GameBoardMember> members = gameBoardMemberRepository.findAll();
        assertThat(members).singleElement().satisfies(member -> {
            assertThat(member.getGameBoard().getId()).isEqualTo(exercise.getGameBoard().getId());
            assertThat(member.getMember()).isNull();
            assertThat(member.getGuest()).isNull();
            assertThat(member.getName()).isEqualTo("김세익");
            assertThat(member.getGender()).isEqualTo(Gender.MALE);
            assertThat(member.getLevel()).isEqualTo(Level.D);
            assertThat(member.getAgeGroup().getKoreanName()).isEqualTo("30대");
            assertThat(member.getShuttlecockSubmitted()).isFalse();
            assertThat(member.getParticipating()).isTrue();
            assertThat(member.getGameCount()).isZero();
        });
    }

    @Test
    @DisplayName("ageGroup을 생략하면 연령대 없이 명단을 생성한다")
    void createMember_allowsMissingAgeGroup() throws Exception {
        authenticate(gameHost);
        String request = """
                {"name":"선수","gender":"여성","level":"A조"}
                """;

        mockMvc.perform(post("/api/game-boards/{gameBoardId}/gameBoardMembers", exercise.getGameBoard().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk());

        assertThat(gameBoardMemberRepository.findAll())
                .singleElement()
                .extracting(GameBoardMember::getAgeGroup)
                .isNull();
    }

    @Test
    @DisplayName("필수 입력값이 없거나 공백이면 400을 반환한다")
    void createMember_rejectsMissingRequiredFields() throws Exception {
        authenticate(gameHost);
        List<String> invalidRequests = List.of(
                "{\"name\":\"   \",\"gender\":\"남성\",\"level\":\"D조\"}",
                "{\"name\":\"선수\",\"level\":\"D조\"}",
                "{\"name\":\"선수\",\"gender\":\"남성\"}",
                "{\"name\":\"" + "가".repeat(256) + "\",\"gender\":\"남성\",\"level\":\"D조\"}");

        for (String request : invalidRequests) {
            mockMvc.perform(post("/api/game-boards/{gameBoardId}/gameBoardMembers", exercise.getGameBoard().getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isBadRequest());
        }
        assertThat(gameBoardMemberRepository.count()).isZero();
    }

    @Test
    @DisplayName("정의되지 않은 성별, 급수, 연령대는 400을 반환한다")
    void createMember_rejectsInvalidKoreanValues() throws Exception {
        authenticate(gameHost);
        List<String> invalidRequests = List.of(
                "{\"name\":\"선수\",\"gender\":\"기타\",\"level\":\"D조\"}",
                "{\"name\":\"선수\",\"gender\":\"남성\",\"level\":\"프로\"}",
                "{\"name\":\"선수\",\"gender\":\"남성\",\"level\":\"D조\",\"ageGroup\":\"80대\"}");

        for (String request : invalidRequests) {
            mockMvc.perform(post("/api/game-boards/{gameBoardId}/gameBoardMembers", exercise.getGameBoard().getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isBadRequest());
        }
        assertThat(gameBoardMemberRepository.count()).isZero();
    }

    @Test
    @DisplayName("게임 진행자가 아닌 사용자는 명단을 생성할 수 없다")
    void createMember_deniesNonGameHost() throws Exception {
        authenticate(otherMember);
        String request = """
                {"name":"선수","gender":"남성","level":"D조","ageGroup":"30대"}
                """;

        mockMvc.perform(post("/api/game-boards/{gameBoardId}/gameBoardMembers", exercise.getGameBoard().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(GameErrorCode.GAME_BOARD_ACCESS_DENIED.getCode()));

        assertThat(gameBoardMemberRepository.count()).isZero();
    }

    private void authenticate(Member member) {
        SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());
    }
}
