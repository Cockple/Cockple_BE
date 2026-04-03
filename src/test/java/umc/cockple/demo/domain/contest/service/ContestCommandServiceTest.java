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
import umc.cockple.demo.domain.contest.dto.*;
import umc.cockple.demo.domain.contest.enums.MedalType;
import umc.cockple.demo.domain.contest.exception.ContestErrorCode;
import umc.cockple.demo.domain.contest.exception.ContestException;
import umc.cockple.demo.domain.contest.repository.ContestRepository;
import umc.cockple.demo.domain.file.service.FileService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.party.enums.ParticipationType;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.fixture.ContestFixture;
import umc.cockple.demo.support.fixture.MemberFixture;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContestCommandService")
class ContestCommandServiceTest {

    @InjectMocks
    private ContestCommandServiceImpl contestCommandService;

    @Mock private ContestRepository contestRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private ContestConverter contestConverter;

    private Member member;

    @BeforeEach
    void setUp() {
        member = MemberFixture.createMember("테스터", Gender.MALE, Level.A, 1001L);
        ReflectionTestUtils.setField(member, "id", 1L);
    }


    @Nested
    @DisplayName("createContestRecord - 대회 기록 등록")
    class CreateContestRecord {

        private ContestRecordCreateDTO.Request request;
        private ContestRecordCreateDTO.Command command;

        @BeforeEach
        void setUp() {
            request = ContestRecordCreateDTO.Request.builder()
                    .contestName("2024 전국 배드민턴 오픈 대회")
                    .date(LocalDate.of(2024, 6, 15))
                    .medalType(MedalType.GOLD)
                    .type(ParticipationType.SINGLE)
                    .level(Level.A)
                    .content("결승에서 승리했습니다.")
                    .contentIsOpen(true)
                    .videoIsOpen(true)
                    .build();

            command = ContestRecordCreateDTO.Command.builder()
                    .memberId(member.getId())
                    .contestName("2024 전국 배드민턴 오픈 대회")
                    .date(LocalDate.of(2024, 6, 15))
                    .medalType(MedalType.GOLD)
                    .type(ParticipationType.SINGLE)
                    .level(Level.A)
                    .content("결승에서 승리했습니다.")
                    .contentIsOpen(true)
                    .videoIsOpen(true)
                    .build();
        }

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("회원이 존재하면 대회 기록을 저장하고 응답을 반환한다")
            void createContestRecord_success() {
                // given
                Contest savedContest = ContestFixture.createContest(member, "2024 전국 배드민턴 오픈 대회", MedalType.GOLD);
                ReflectionTestUtils.setField(savedContest, "id", 10L);

                ContestRecordCreateDTO.Response expectedResponse = ContestRecordCreateDTO.Response.builder()
                        .contestId(10L)
                        .createdAt(LocalDateTime.now())
                        .contestImgIds(List.of())
                        .contestVideoIds(List.of())
                        .build();

                given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));
                given(contestConverter.toCreateCommand(request, member.getId())).willReturn(command);
                given(contestRepository.save(any())).willReturn(savedContest);
                given(contestConverter.toCreateResponseDTO(savedContest)).willReturn(expectedResponse);

                // when
                ContestRecordCreateDTO.Response response =
                        contestCommandService.createContestRecord(member.getId(), request);

                // then
                assertThat(response.contestId()).isEqualTo(10L);
                then(contestRepository).should().save(any());
                then(contestConverter).should().toCreateResponseDTO(savedContest);
            }

            @Test
            @DisplayName("이미지를 포함하면 contestImgIds 목록을 반환한다")
            void createContestRecord_withImages() {
                // given
                ContestRecordCreateDTO.Request requestWithImgs = ContestRecordCreateDTO.Request.builder()
                        .contestName("이미지 포함 대회")
                        .type(ParticipationType.SINGLE)
                        .level(Level.A)
                        .contentIsOpen(true)
                        .videoIsOpen(false)
                        .contestImgs(List.of(
                                new AddContestImgRequest("img1.jpg", 1),
                                new AddContestImgRequest("img2.jpg", 2)
                        ))
                        .build();

                ContestRecordCreateDTO.Command commandWithImgs = ContestRecordCreateDTO.Command.builder()
                        .memberId(member.getId())
                        .contestName("이미지 포함 대회")
                        .type(ParticipationType.SINGLE)
                        .level(Level.A)
                        .contentIsOpen(true)
                        .videoIsOpen(false)
                        .contestImgs(List.of(
                                new AddContestImgRequest("img1.jpg", 1),
                                new AddContestImgRequest("img2.jpg", 2)
                        ))
                        .build();

                Contest savedContest = ContestFixture.createContest(member, "이미지 포함 대회", MedalType.NONE);
                ReflectionTestUtils.setField(savedContest, "id", 20L);

                ContestRecordCreateDTO.Response expectedResponse = ContestRecordCreateDTO.Response.builder()
                        .contestId(20L)
                        .createdAt(LocalDateTime.now())
                        .contestImgIds(List.of(1L, 2L))
                        .contestVideoIds(List.of())
                        .build();

                given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));
                given(contestConverter.toCreateCommand(requestWithImgs, member.getId())).willReturn(commandWithImgs);
                given(contestRepository.save(any())).willReturn(savedContest);
                given(contestConverter.toCreateResponseDTO(savedContest)).willReturn(expectedResponse);

                // when
                ContestRecordCreateDTO.Response response =
                        contestCommandService.createContestRecord(member.getId(), requestWithImgs);

                // then
                assertThat(response.contestImgIds()).hasSize(2);
            }

            @Test
            @DisplayName("영상을 포함하면 contestVideoIds 목록을 반환한다")
            void createContestRecord_withVideos() {
                // given
                ContestRecordCreateDTO.Request requestWithVideos = ContestRecordCreateDTO.Request.builder()
                        .contestName("영상 포함 대회")
                        .type(ParticipationType.SINGLE)
                        .level(Level.A)
                        .contentIsOpen(true)
                        .videoIsOpen(true)
                        .contestVideos(List.of(
                                new AddContestVideoRequest("videos/match1.mp4", 1),
                                new AddContestVideoRequest("videos/match2.mp4", 2)
                        ))
                        .build();

                ContestRecordCreateDTO.Command commandWithVideos = ContestRecordCreateDTO.Command.builder()
                        .memberId(member.getId())
                        .contestName("영상 포함 대회")
                        .type(ParticipationType.SINGLE)
                        .level(Level.A)
                        .contentIsOpen(true)
                        .videoIsOpen(true)
                        .contestVideos(List.of(
                                new AddContestVideoRequest("videos/match1.mp4", 1),
                                new AddContestVideoRequest("videos/match2.mp4", 2)
                        ))
                        .build();

                Contest savedContest = ContestFixture.createContest(member, "영상 포함 대회", MedalType.NONE);
                ReflectionTestUtils.setField(savedContest, "id", 30L);

                ContestRecordCreateDTO.Response expectedResponse = ContestRecordCreateDTO.Response.builder()
                        .contestId(30L)
                        .createdAt(LocalDateTime.now())
                        .contestImgIds(List.of())
                        .contestVideoIds(List.of(1L, 2L))
                        .build();

                given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));
                given(contestConverter.toCreateCommand(requestWithVideos, member.getId())).willReturn(commandWithVideos);
                given(contestRepository.save(any())).willReturn(savedContest);
                given(contestConverter.toCreateResponseDTO(savedContest)).willReturn(expectedResponse);

                // when
                ContestRecordCreateDTO.Response response =
                        contestCommandService.createContestRecord(member.getId(), requestWithVideos);

                // then
                assertThat(response.contestVideoIds()).hasSize(2);
                assertThat(response.contestImgIds()).isEmpty();
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("존재하지 않는 회원이면 ContestException(MEMBER_NOT_FOUND)을 던진다")
            void createContestRecord_memberNotFound() {
                // given
                given(memberRepository.findById(999L)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() ->
                        contestCommandService.createContestRecord(999L, request))
                        .isInstanceOf(ContestException.class)
                        .satisfies(e -> assertThat(((ContestException) e).getCode())
                                .isEqualTo(ContestErrorCode.MEMBER_NOT_FOUND));
            }

            @Test
            @DisplayName("이미지가 3개를 초과하면 ContestException(IMAGE_UPLOAD_LIMIT_EXCEEDED)을 던진다")
            void createContestRecord_imageLimitExceeded() {
                // given
                ContestRecordCreateDTO.Command commandWith4Imgs = ContestRecordCreateDTO.Command.builder()
                        .memberId(member.getId())
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

                given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));
                given(contestConverter.toCreateCommand(any(), any())).willReturn(commandWith4Imgs);

                // when & then
                assertThatThrownBy(() ->
                        contestCommandService.createContestRecord(member.getId(), request))
                        .isInstanceOf(ContestException.class)
                        .satisfies(e -> assertThat(((ContestException) e).getCode())
                                .isEqualTo(ContestErrorCode.IMAGE_UPLOAD_LIMIT_EXCEEDED));
            }
        }
    }

    // =========================================================
    // updateContestRecord
    // =========================================================

    @Nested
    @DisplayName("updateContestRecord - 대회 기록 수정")
    class UpdateContestRecord {

        private Contest contest;
        private ContestRecordUpdateDTO.Request request;

        @BeforeEach
        void setUp() {
            contest = ContestFixture.createContest(member, "원래 대회 이름", MedalType.NONE);
            ReflectionTestUtils.setField(contest, "id", 10L);

            request = new ContestRecordUpdateDTO.Request(
                    "수정된 대회 이름",
                    LocalDate.of(2024, 9, 20),
                    MedalType.SILVER,
                    ParticipationType.MIX_DOUBLES,
                    Level.B,
                    "수정된 후기",
                    true,
                    false,
                    null,
                    null
            );
        }

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("대회 기록이 존재하면 수정 후 응답을 반환한다")
            void updateContestRecord_success() {
                // given
                ContestRecordUpdateDTO.Response expectedResponse = ContestRecordUpdateDTO.Response.builder()
                        .contestId(10L)
                        .contestImgs(List.of())
                        .contestVideos(List.of())
                        .UpdatedAt(LocalDateTime.now())
                        .build();

                given(contestRepository.findByIdAndMember_Id(10L, member.getId()))
                        .willReturn(Optional.of(contest));
                given(contestRepository.save(contest)).willReturn(contest);
                given(contestConverter.toUpdateResponseDTO(contest)).willReturn(expectedResponse);

                // when
                ContestRecordUpdateDTO.Response response =
                        contestCommandService.updateContestRecord(member.getId(), 10L, request);

                // then
                assertThat(response.contestId()).isEqualTo(10L);
                then(contestRepository).should().save(contest);
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("존재하지 않거나 본인 소유가 아닌 대회 기록이면 ContestException(CONTEST_NOT_FOUND)을 던진다")
            void updateContestRecord_contestNotFound() {
                // given
                given(contestRepository.findByIdAndMember_Id(999L, member.getId()))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() ->
                        contestCommandService.updateContestRecord(member.getId(), 999L, request))
                        .isInstanceOf(ContestException.class)
                        .satisfies(e -> assertThat(((ContestException) e).getCode())
                                .isEqualTo(ContestErrorCode.CONTEST_NOT_FOUND));
            }

            @Test
            @DisplayName("수정 요청 이미지가 3개를 초과하면 ContestException(IMAGE_UPLOAD_LIMIT_EXCEEDED)을 던진다")
            void updateContestRecord_imageLimitExceeded() {
                // given
                ContestRecordUpdateDTO.Request requestWith4Imgs = new ContestRecordUpdateDTO.Request(
                        "수정된 이름", null, MedalType.GOLD, ParticipationType.SINGLE, Level.A,
                        null, true, false,
                        List.of(
                                new ContestImgUpdateRequest(null, "img1.jpg", 1),
                                new ContestImgUpdateRequest(null, "img2.jpg", 2),
                                new ContestImgUpdateRequest(null, "img3.jpg", 3),
                                new ContestImgUpdateRequest(null, "img4.jpg", 4)
                        ),
                        null
                );

                given(contestRepository.findByIdAndMember_Id(10L, member.getId()))
                        .willReturn(Optional.of(contest));

                // when & then
                assertThatThrownBy(() ->
                        contestCommandService.updateContestRecord(member.getId(), 10L, requestWith4Imgs))
                        .isInstanceOf(ContestException.class)
                        .satisfies(e -> assertThat(((ContestException) e).getCode())
                                .isEqualTo(ContestErrorCode.IMAGE_UPLOAD_LIMIT_EXCEEDED));
            }
        }
    }

    // =========================================================
    // deleteContestRecord
    // =========================================================

    @Nested
    @DisplayName("deleteContestRecord - 대회 기록 삭제")
    class DeleteContestRecord {

        private Contest contest;

        @BeforeEach
        void setUp() {
            contest = ContestFixture.createContest(member, "삭제할 대회", MedalType.BRONZE);
            ReflectionTestUtils.setField(contest, "id", 10L);
        }

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("대회 기록이 존재하면 삭제 후 deleteContestId를 반환한다")
            void deleteContestRecord_success() {
                // given
                ContestRecordDeleteDTO.Response expectedResponse = ContestRecordDeleteDTO.Response.builder()
                        .deleteContestId(10L)
                        .build();

                given(contestRepository.findByIdAndMember_Id(10L, member.getId()))
                        .willReturn(Optional.of(contest));
                given(contestConverter.toDeleteResponseDTO(contest)).willReturn(expectedResponse);

                // when
                ContestRecordDeleteDTO.Response response =
                        contestCommandService.deleteContestRecord(member.getId(), 10L);

                // then
                assertThat(response.deleteContestId()).isEqualTo(10L);
                then(contestRepository).should().delete(contest);
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("존재하지 않거나 본인 소유가 아닌 대회 기록이면 ContestException(CONTEST_NOT_FOUND)을 던진다")
            void deleteContestRecord_contestNotFound() {
                // given
                given(contestRepository.findByIdAndMember_Id(999L, member.getId()))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() ->
                        contestCommandService.deleteContestRecord(member.getId(), 999L))
                        .isInstanceOf(ContestException.class)
                        .satisfies(e -> assertThat(((ContestException) e).getCode())
                                .isEqualTo(ContestErrorCode.CONTEST_NOT_FOUND));
            }
        }
    }
}
