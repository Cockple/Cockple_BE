package umc.cockple.demo.domain.contest.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import umc.cockple.demo.domain.contest.converter.ContestConverter;
import umc.cockple.demo.domain.contest.domain.Contest;
import umc.cockple.demo.domain.contest.domain.ContestVideo;
import umc.cockple.demo.domain.contest.dto.ContestMedalSummaryDTO;
import umc.cockple.demo.domain.contest.dto.ContestRecordDetailDTO;
import umc.cockple.demo.domain.contest.dto.ContestRecordSimpleDTO;
import umc.cockple.demo.domain.contest.enums.MedalType;
import umc.cockple.demo.domain.contest.exception.ContestErrorCode;
import umc.cockple.demo.domain.contest.exception.ContestException;
import umc.cockple.demo.domain.contest.repository.ContestRepository;
import umc.cockple.demo.domain.contest.repository.MedalCountProjection;
import umc.cockple.demo.domain.file.service.FileService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.party.enums.ParticipationType;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.fixture.ContestFixture;
import umc.cockple.demo.support.fixture.MemberFixture;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContestQueryService")
class ContestQueryServiceTest {

    @InjectMocks
    private ContestQueryServiceImpl contestQueryService;

    @Mock private ContestRepository contestRepository;
    @Mock private ContestConverter contestConverter;
    @Mock private FileService fileService;

    private Member member;
    private Member otherMember;

    @BeforeEach
    void setUp() {
        member = MemberFixture.createMember("테스터", Gender.MALE, Level.A, 1001L);
        ReflectionTestUtils.setField(member, "id", 1L);

        otherMember = MemberFixture.createMember("다른회원", Gender.FEMALE, Level.B, 2002L);
        ReflectionTestUtils.setField(otherMember, "id", 2L);
    }


    @Nested
    @DisplayName("getContestRecordDetail - 대회 기록 상세 조회")
    class GetContestRecordDetail {

        private Contest publicContest;
        private Contest privateContest;
        private Contest publicVideoContest;

        @BeforeEach
        void setUp() {
            publicContest = ContestFixture.createContest(member, "공개 대회", MedalType.GOLD);
            ReflectionTestUtils.setField(publicContest, "id", 10L);

            privateContest = ContestFixture.createPrivateContest(
                    member, "비공개 대회", MedalType.SILVER,
                    LocalDate.of(2024, 3, 10), ParticipationType.SINGLE, Level.A, "비공개 후기"
            );
            ReflectionTestUtils.setField(privateContest, "id", 20L);

            // videoIsOpen=true, 영상 1개 포함
            publicVideoContest = ContestFixture.createContest(member, "영상 공개 대회", MedalType.BRONZE);
            ReflectionTestUtils.setField(publicVideoContest, "id", 30L);
            ContestVideo video = ContestVideo.of(publicVideoContest, "https://cdn.example.com/match.mp4", 1);
            ReflectionTestUtils.setField(video, "id", 100L);
        }

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("본인 조회 시 content와 영상 목록을 그대로 반환한다")
            void getDetail_owner() {
                // given
                ContestRecordDetailDTO.Response expectedResponse = ContestRecordDetailDTO.Response.builder()
                        .contestId(20L)
                        .content("비공개 후기")
                        .contestVideoIds(List.of())
                        .contestVideoUrls(List.of())
                        .build();

                given(contestRepository.findByIdAndMember_Id(20L, member.getId()))
                        .willReturn(Optional.of(privateContest));
                given(contestConverter.toDetailResponseDTO(eq(privateContest), any(), any(), any(), any(), eq("비공개 후기")))
                        .willReturn(expectedResponse);

                // when
                ContestRecordDetailDTO.Response response =
                        contestQueryService.getContestRecordDetail(member.getId(), member.getId(), 20L);

                // then
                assertThat(response.content()).isEqualTo("비공개 후기");
                assertThat(response.contestVideoIds()).isEmpty();
            }

            @Test
            @DisplayName("타인 조회 + contentIsOpen=true이면 content를 반환한다")
            void getDetail_otherMember_publicContent() {
                // given
                ContestRecordDetailDTO.Response expectedResponse = ContestRecordDetailDTO.Response.builder()
                        .contestId(10L)
                        .content("대회 후기입니다.")
                        .build();

                given(contestRepository.findByIdAndMember_Id(10L, member.getId()))
                        .willReturn(Optional.of(publicContest));
                given(contestConverter.toDetailResponseDTO(eq(publicContest), any(), any(), any(), any(), eq("대회 후기입니다.")))
                        .willReturn(expectedResponse);

                // when
                ContestRecordDetailDTO.Response response =
                        contestQueryService.getContestRecordDetail(otherMember.getId(), member.getId(), 10L);

                // then
                assertThat(response.content()).isEqualTo("대회 후기입니다.");
            }

            @Test
            @DisplayName("타인 조회 + contentIsOpen=false이면 content를 빈 문자열로 반환한다")
            void getDetail_otherMember_privateContent() {
                // given
                ContestRecordDetailDTO.Response expectedResponse = ContestRecordDetailDTO.Response.builder()
                        .contestId(20L)
                        .content("")
                        .contestVideoIds(List.of())
                        .contestVideoUrls(List.of())
                        .build();

                given(contestRepository.findByIdAndMember_Id(20L, member.getId()))
                        .willReturn(Optional.of(privateContest));
                given(contestConverter.toDetailResponseDTO(eq(privateContest), any(), any(), any(), any(), eq("")))
                        .willReturn(expectedResponse);

                // when
                ContestRecordDetailDTO.Response response =
                        contestQueryService.getContestRecordDetail(otherMember.getId(), member.getId(), 20L);

                // then
                assertThat(response.content()).isEmpty();
                assertThat(response.contestVideoIds()).isEmpty();
            }

            @Test
            @DisplayName("타인 조회 + videoIsOpen=true이면 영상 ID와 URL 목록이 converter에 전달된다")
            void getDetail_otherMember_publicVideo() {
                // given
                ContestRecordDetailDTO.Response expectedResponse = ContestRecordDetailDTO.Response.builder()
                        .contestId(30L)
                        .contestVideoIds(List.of(100L))
                        .contestVideoUrls(List.of("https://cdn.example.com/match.mp4"))
                        .build();

                given(contestRepository.findByIdAndMember_Id(30L, member.getId()))
                        .willReturn(Optional.of(publicVideoContest));
                given(contestConverter.toDetailResponseDTO(
                        eq(publicVideoContest), any(), any(),
                        eq(List.of(100L)),
                        eq(List.of("https://cdn.example.com/match.mp4")),
                        any()))
                        .willReturn(expectedResponse);

                // when
                ContestRecordDetailDTO.Response response =
                        contestQueryService.getContestRecordDetail(otherMember.getId(), member.getId(), 30L);

                // then
                assertThat(response.contestVideoIds()).containsExactly(100L);
                assertThat(response.contestVideoUrls()).containsExactly("https://cdn.example.com/match.mp4");
            }

            @Test
            @DisplayName("타인 조회 + videoIsOpen=false이면 빈 영상 목록이 converter에 전달된다")
            void getDetail_otherMember_privateVideo() {
                // given
                ContestRecordDetailDTO.Response expectedResponse = ContestRecordDetailDTO.Response.builder()
                        .contestId(20L)
                        .content("")
                        .contestVideoIds(List.of())
                        .contestVideoUrls(List.of())
                        .build();

                given(contestRepository.findByIdAndMember_Id(20L, member.getId()))
                        .willReturn(Optional.of(privateContest));
                given(contestConverter.toDetailResponseDTO(
                        eq(privateContest), any(), any(),
                        eq(List.of()),
                        eq(List.of()),
                        any()))
                        .willReturn(expectedResponse);

                // when
                ContestRecordDetailDTO.Response response =
                        contestQueryService.getContestRecordDetail(otherMember.getId(), member.getId(), 20L);

                // then
                then(contestConverter).should().toDetailResponseDTO(
                        eq(privateContest), any(), any(),
                        eq(List.of()), eq(List.of()), any());
                assertThat(response.contestVideoIds()).isEmpty();
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("존재하지 않는 대회 기록이면 ContestException(CONTEST_NOT_FOUND)을 던진다")
            void getDetail_notFound() {
                // given
                given(contestRepository.findByIdAndMember_Id(999L, member.getId()))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() ->
                        contestQueryService.getContestRecordDetail(member.getId(), member.getId(), 999L))
                        .isInstanceOf(ContestException.class)
                        .satisfies(e -> assertThat(((ContestException) e).getCode())
                                .isEqualTo(ContestErrorCode.CONTEST_NOT_FOUND));
            }
        }
    }


    @Nested
    @DisplayName("getMyContestRecordsByMedalType - 내 대회 기록 리스트 조회 (전체, 미입상)")
    class GetMyContestRecordsByMedalType {

        private Contest goldContest;
        private Contest silverContest;
        private Contest noneContest;

        @BeforeEach
        void setUp() {
            goldContest = ContestFixture.createContest(member, "금메달 대회", MedalType.GOLD);
            ReflectionTestUtils.setField(goldContest, "id", 1L);

            silverContest = ContestFixture.createContest(member, "은메달 대회", MedalType.SILVER);
            ReflectionTestUtils.setField(silverContest, "id", 2L);

            noneContest = ContestFixture.createContest(member, "미입상 대회", MedalType.NONE);
            ReflectionTestUtils.setField(noneContest, "id", 3L);
        }

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("medalType=null이면 전체 대회 기록을 반환한다")
            void getList_noFilter() {
                // given
                given(fileService.getUrlFromKey(anyString())).willReturn("https://cdn.example.com/medal.svg");
                given(contestRepository.findAllByMember_Id(member.getId()))
                        .willReturn(List.of(goldContest, silverContest, noneContest));

                // when
                List<ContestRecordSimpleDTO.Response> result =
                        contestQueryService.getMyContestRecordsByMedalType(member.getId(), null);

                // then
                assertThat(result).hasSize(3);
            }

            @Test
            @DisplayName("medalType=NONE이면 미입상 대회 기록만 반환한다")
            void getList_filterByNone() {
                // given
                given(fileService.getUrlFromKey(anyString())).willReturn("https://cdn.example.com/medal.svg");
                given(contestRepository.findAllByMember_Id(member.getId()))
                        .willReturn(List.of(goldContest, silverContest, noneContest));

                // when
                List<ContestRecordSimpleDTO.Response> result =
                        contestQueryService.getMyContestRecordsByMedalType(member.getId(), MedalType.NONE);

                // then
                assertThat(result).hasSize(1);
                assertThat(result.get(0).contestName()).isEqualTo("미입상 대회");
            }

            @Test
            @DisplayName("medalType=GOLD이면 NONE 필터링 없이 전체를 반환한다")
            void getList_filterByGold_returnsAll() {
                // given
                given(fileService.getUrlFromKey(anyString())).willReturn("https://cdn.example.com/medal.svg");
                given(contestRepository.findAllByMember_Id(member.getId()))
                        .willReturn(List.of(goldContest, silverContest, noneContest));

                // when
                List<ContestRecordSimpleDTO.Response> result =
                        contestQueryService.getMyContestRecordsByMedalType(member.getId(), MedalType.GOLD);

                // then
                assertThat(result).hasSize(3);
            }

            @Test
            @DisplayName("medalType=SILVER이면 NONE 필터링 없이 전체를 반환한다")
            void getList_filterBySilver_returnsAll() {
                // given
                given(fileService.getUrlFromKey(anyString())).willReturn("https://cdn.example.com/medal.svg");
                given(contestRepository.findAllByMember_Id(member.getId()))
                        .willReturn(List.of(goldContest, silverContest, noneContest));

                // when
                List<ContestRecordSimpleDTO.Response> result =
                        contestQueryService.getMyContestRecordsByMedalType(member.getId(), MedalType.SILVER);

                // then
                assertThat(result).hasSize(3);
            }

            @Test
            @DisplayName("medalType=BRONZE이면 NONE 필터링 없이 전체를 반환한다")
            void getList_filterByBronze_returnsAll() {
                // given
                given(fileService.getUrlFromKey(anyString())).willReturn("https://cdn.example.com/medal.svg");
                given(contestRepository.findAllByMember_Id(member.getId()))
                        .willReturn(List.of(goldContest, silverContest, noneContest));

                // when
                List<ContestRecordSimpleDTO.Response> result =
                        contestQueryService.getMyContestRecordsByMedalType(member.getId(), MedalType.BRONZE);

                // then
                assertThat(result).hasSize(3);
            }

            @Test
            @DisplayName("대회 기록이 없으면 빈 리스트를 반환한다")
            void getList_empty() {
                // given
                given(contestRepository.findAllByMember_Id(member.getId()))
                        .willReturn(List.of());

                // when
                List<ContestRecordSimpleDTO.Response> result =
                        contestQueryService.getMyContestRecordsByMedalType(member.getId(), null);

                // then
                assertThat(result).isEmpty();
                then(fileService).shouldHaveNoInteractions();
            }
        }
    }


    @Nested
    @DisplayName("getMyMedalSummary - 내 대회 메달 조회")
    class GetMyMedalSummary {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("메달이 있으면 종류별 개수와 합계를 반환한다")
            void getMedalSummary_hasMedals() {
                // given
                ContestMedalSummaryDTO.Response expectedResponse = ContestMedalSummaryDTO.Response.builder()
                        .myMedalTotal(4)
                        .goldCount(2)
                        .silverCount(1)
                        .bronzeCount(1)
                        .build();

                MedalCountProjection counts = mock(MedalCountProjection.class);
                given(contestRepository.countMedalsByMemberId(member.getId())).willReturn(counts);
                given(contestConverter.toMedalSummaryResponseDTO(counts)).willReturn(expectedResponse);

                // when
                ContestMedalSummaryDTO.Response response =
                        contestQueryService.getMyMedalSummary(member.getId());

                // then
                assertThat(response.myMedalTotal()).isEqualTo(4);
                assertThat(response.goldCount()).isEqualTo(2);
                assertThat(response.silverCount()).isEqualTo(1);
                assertThat(response.bronzeCount()).isEqualTo(1);
            }

            @Test
            @DisplayName("메달이 없으면 모두 0을 반환한다")
            void getMedalSummary_noMedals() {
                // given
                ContestMedalSummaryDTO.Response expectedResponse = ContestMedalSummaryDTO.Response.builder()
                        .myMedalTotal(0)
                        .goldCount(0)
                        .silverCount(0)
                        .bronzeCount(0)
                        .build();

                MedalCountProjection counts = mock(MedalCountProjection.class);
                given(contestRepository.countMedalsByMemberId(member.getId())).willReturn(counts);
                given(contestConverter.toMedalSummaryResponseDTO(counts)).willReturn(expectedResponse);

                // when
                ContestMedalSummaryDTO.Response response =
                        contestQueryService.getMyMedalSummary(member.getId());

                // then
                assertThat(response.myMedalTotal()).isEqualTo(0);
                assertThat(response.goldCount()).isEqualTo(0);
                assertThat(response.silverCount()).isEqualTo(0);
                assertThat(response.bronzeCount()).isEqualTo(0);
            }
        }
    }
}
