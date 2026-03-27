package umc.cockple.demo.domain.bookmark.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import umc.cockple.demo.domain.bookmark.converter.BookmarkConverter;
import umc.cockple.demo.domain.bookmark.domain.ExerciseBookmark;
import umc.cockple.demo.domain.bookmark.domain.PartyBookmark;
import umc.cockple.demo.domain.bookmark.dto.GetAllExerciseBookmarksResponseDTO;
import umc.cockple.demo.domain.bookmark.dto.GetAllPartyBookmarkResponseDTO;
import umc.cockple.demo.domain.bookmark.enums.BookmarkedExerciseOrderType;
import umc.cockple.demo.domain.bookmark.repository.ExerciseBookmarkRepository;
import umc.cockple.demo.domain.bookmark.repository.PartyBookmarkRepository;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.file.service.FileService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.exception.MemberException;
import umc.cockple.demo.domain.member.repository.MemberExerciseRepository;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.enums.ActivityTime;
import umc.cockple.demo.domain.party.enums.PartyOrderType;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.fixture.ExerciseFixture;
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookmarkQueryService")
class BookmarkQueryServiceTest {

    @InjectMocks
    private BookmarkQueryService bookmarkQueryService;

    @Mock private ExerciseBookmarkRepository exerciseBookmarkRepository;
    @Mock private PartyBookmarkRepository partyBookmarkRepository;
    @Mock private MemberPartyRepository memberPartyRepository;
    @Mock private MemberExerciseRepository memberExerciseRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private BookmarkConverter bookmarkConverter;

    private Member member;
    private Party party;

    @BeforeEach
    void setUp() {
        member = MemberFixture.createMember("테스트 유저", Gender.MALE, Level.A, 1001L);
        ReflectionTestUtils.setField(member, "id", 1L);

        party = PartyFixture.createParty("테스트 모임", member.getId(),
                PartyFixture.createPartyAddr("서울특별시", "강남구"));
        ReflectionTestUtils.setField(party, "id", 10L);
    }

    @Nested
    @DisplayName("getAllExerciseBookmarks - 찜한 운동 목록 조회")
    class GetAllExerciseBookmarks {

        private Exercise oldExercise;
        private Exercise newExercise;

        @BeforeEach
        void setUp() {
            oldExercise = ExerciseFixture.createExerciseWithAddr(party, LocalDate.of(2099, 6, 1));
            ReflectionTestUtils.setField(oldExercise, "id", 101L);

            newExercise = ExerciseFixture.createExerciseWithAddr(party, LocalDate.of(2099, 12, 31));
            ReflectionTestUtils.setField(newExercise, "id", 102L);
        }

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("찜한 운동이 없으면 빈 목록을 반환한다")
            void noBookmarks_returnsEmptyList() {
                // given
                given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));
                given(exerciseBookmarkRepository.findAllByMember(member)).willReturn(new ArrayList<>());
                given(memberPartyRepository.findAllPartyIdsByMemberAndPartyIds(anyLong(), anyList()))
                        .willReturn(new ArrayList<>());
                given(memberExerciseRepository.findAllExerciseIdsByMemberAndExerciseIds(anyLong(), anyList()))
                        .willReturn(new ArrayList<>());

                // when
                List<GetAllExerciseBookmarksResponseDTO> result =
                        bookmarkQueryService.getAllExerciseBookmarks(member.getId(), BookmarkedExerciseOrderType.LATEST);

                // then
                assertThat(result).isEmpty();
            }

            @Test
            @DisplayName("LATEST 정렬 시 최신순으로 반환한다")
            void latestOrder_returnsNewestFirst() {
                // given
                ExerciseBookmark bookmarkOld = ExerciseBookmark.builder()
                        .member(member).exercise(oldExercise).build();
                ReflectionTestUtils.setField(bookmarkOld, "createdAt", LocalDateTime.now().minusDays(2));

                ExerciseBookmark bookmarkNew = ExerciseBookmark.builder()
                        .member(member).exercise(newExercise).build();
                ReflectionTestUtils.setField(bookmarkNew, "createdAt", LocalDateTime.now().minusDays(1));

                // 레포지토리에서 오래된 순서로 반환
                List<ExerciseBookmark> bookmarks = new ArrayList<>(List.of(bookmarkOld, bookmarkNew));

                GetAllExerciseBookmarksResponseDTO dtoOld = GetAllExerciseBookmarksResponseDTO.builder()
                        .exerciseId(101L).partyName("테스트 모임").build();
                GetAllExerciseBookmarksResponseDTO dtoNew = GetAllExerciseBookmarksResponseDTO.builder()
                        .exerciseId(102L).partyName("테스트 모임").build();

                given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));
                given(exerciseBookmarkRepository.findAllByMember(member)).willReturn(bookmarks);
                given(memberPartyRepository.findAllPartyIdsByMemberAndPartyIds(anyLong(), anyList()))
                        .willReturn(List.of(party.getId()));
                given(memberExerciseRepository.findAllExerciseIdsByMemberAndExerciseIds(anyLong(), anyList()))
                        .willReturn(List.of(oldExercise.getId(), newExercise.getId()));
                given(bookmarkConverter.exerciseBookmarkToDTO(eq(bookmarkNew), any(Boolean.class), any(Boolean.class)))
                        .willReturn(dtoNew);
                given(bookmarkConverter.exerciseBookmarkToDTO(eq(bookmarkOld), any(Boolean.class), any(Boolean.class)))
                        .willReturn(dtoOld);

                // when
                List<GetAllExerciseBookmarksResponseDTO> result =
                        bookmarkQueryService.getAllExerciseBookmarks(member.getId(), BookmarkedExerciseOrderType.LATEST);

                // then
                assertThat(result).hasSize(2);
                assertThat(result.get(0).exerciseId()).isEqualTo(102L); // 최신 것이 먼저
                assertThat(result.get(1).exerciseId()).isEqualTo(101L);
            }

            @Test
            @DisplayName("EARLIEST 정렬 시 오래된 순으로 반환한다")
            void earliestOrder_returnsOldestFirst() {
                // given
                ExerciseBookmark bookmarkOld = ExerciseBookmark.builder()
                        .member(member).exercise(oldExercise).build();
                ReflectionTestUtils.setField(bookmarkOld, "createdAt", LocalDateTime.now().minusDays(2));

                ExerciseBookmark bookmarkNew = ExerciseBookmark.builder()
                        .member(member).exercise(newExercise).build();
                ReflectionTestUtils.setField(bookmarkNew, "createdAt", LocalDateTime.now().minusDays(1));

                // 레포지토리에서 최신 순서로 반환
                List<ExerciseBookmark> bookmarks = new ArrayList<>(List.of(bookmarkNew, bookmarkOld));

                GetAllExerciseBookmarksResponseDTO dtoOld = GetAllExerciseBookmarksResponseDTO.builder()
                        .exerciseId(101L).partyName("테스트 모임").build();
                GetAllExerciseBookmarksResponseDTO dtoNew = GetAllExerciseBookmarksResponseDTO.builder()
                        .exerciseId(102L).partyName("테스트 모임").build();

                given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));
                given(exerciseBookmarkRepository.findAllByMember(member)).willReturn(bookmarks);
                given(memberPartyRepository.findAllPartyIdsByMemberAndPartyIds(anyLong(), anyList()))
                        .willReturn(List.of(party.getId()));
                given(memberExerciseRepository.findAllExerciseIdsByMemberAndExerciseIds(anyLong(), anyList()))
                        .willReturn(List.of(oldExercise.getId(), newExercise.getId()));
                given(bookmarkConverter.exerciseBookmarkToDTO(eq(bookmarkOld), any(Boolean.class), any(Boolean.class)))
                        .willReturn(dtoOld);
                given(bookmarkConverter.exerciseBookmarkToDTO(eq(bookmarkNew), any(Boolean.class), any(Boolean.class)))
                        .willReturn(dtoNew);

                // when
                List<GetAllExerciseBookmarksResponseDTO> result =
                        bookmarkQueryService.getAllExerciseBookmarks(member.getId(), BookmarkedExerciseOrderType.EARLIEST);

                // then
                assertThat(result).hasSize(2);
                assertThat(result.get(0).exerciseId()).isEqualTo(101L); // 오래된 것이 먼저
                assertThat(result.get(1).exerciseId()).isEqualTo(102L);
            }

            @Test
            @DisplayName("includeParty, includeExercise 정보를 정확히 반영하여 변환한다")
            void convertsBookmarkWithCorrectIncludeFlags() {
                // given
                Exercise exercise = ExerciseFixture.createExerciseWithAddr(party, LocalDate.of(2099, 12, 31));
                ReflectionTestUtils.setField(exercise, "id", 101L);

                ExerciseBookmark bookmark = ExerciseBookmark.builder()
                        .member(member).exercise(exercise).build();
                ReflectionTestUtils.setField(bookmark, "createdAt", LocalDateTime.now());

                GetAllExerciseBookmarksResponseDTO dto = GetAllExerciseBookmarksResponseDTO.builder()
                        .exerciseId(101L).includeParty(true).includeExercise(false).build();

                given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));
                given(exerciseBookmarkRepository.findAllByMember(member)).willReturn(new ArrayList<>(List.of(bookmark)));
                given(memberPartyRepository.findAllPartyIdsByMemberAndPartyIds(eq(member.getId()), anyList()))
                        .willReturn(List.of(party.getId())); // 모임 멤버
                given(memberExerciseRepository.findAllExerciseIdsByMemberAndExerciseIds(eq(member.getId()), anyList()))
                        .willReturn(new ArrayList<>()); // 운동 미참여
                given(bookmarkConverter.exerciseBookmarkToDTO(bookmark, true, false)).willReturn(dto);

                // when
                List<GetAllExerciseBookmarksResponseDTO> result =
                        bookmarkQueryService.getAllExerciseBookmarks(member.getId(), BookmarkedExerciseOrderType.LATEST);

                // then
                assertThat(result).hasSize(1);
                assertThat(result.get(0).includeParty()).isTrue();
                assertThat(result.get(0).includeExercise()).isFalse();
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("존재하지 않는 회원이면 MemberException(MEMBER_NOT_FOUND)을 던진다")
            void memberNotFound_throwsMemberException() {
                given(memberRepository.findById(999L)).willReturn(Optional.empty());

                assertThatThrownBy(() ->
                        bookmarkQueryService.getAllExerciseBookmarks(999L, BookmarkedExerciseOrderType.LATEST))
                        .isInstanceOf(MemberException.class)
                        .satisfies(e -> assertThat(((MemberException) e).getCode())
                                .isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND));
            }
        }
    }

    @Nested
    @DisplayName("getAllPartyBookmarks - 찜한 모임 목록 조회")
    class GetAllPartyBookmarks {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("찜한 모임이 없으면 빈 목록을 반환한다")
            void noBookmarks_returnsEmptyList() {
                // given
                given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));
                given(partyBookmarkRepository.findAllByMemberWithParty(member)).willReturn(new ArrayList<>());

                // when
                List<GetAllPartyBookmarkResponseDTO> result =
                        bookmarkQueryService.getAllPartyBookmarks(member.getId(), PartyOrderType.LATEST);

                // then
                assertThat(result).isEmpty();
            }

            @Test
            @DisplayName("LATEST 정렬 시 최신순으로 반환한다")
            void latestOrder_returnsNewestFirst() {
                // given
                Party partyA = PartyFixture.createParty("모임A", member.getId(),
                        PartyFixture.createPartyAddr("서울특별시", "강남구"));
                ReflectionTestUtils.setField(partyA, "id", 11L);

                Party partyB = PartyFixture.createParty("모임B", member.getId(),
                        PartyFixture.createPartyAddr("서울특별시", "종로구"));
                ReflectionTestUtils.setField(partyB, "id", 12L);

                PartyBookmark bookmarkOld = PartyBookmark.builder()
                        .member(member).party(partyA)
                        .orderType(umc.cockple.demo.domain.party.enums.PartyOrderType.LATEST).build();
                ReflectionTestUtils.setField(bookmarkOld, "createdAt", LocalDateTime.now().minusDays(2));

                PartyBookmark bookmarkNew = PartyBookmark.builder()
                        .member(member).party(partyB)
                        .orderType(umc.cockple.demo.domain.party.enums.PartyOrderType.LATEST).build();
                ReflectionTestUtils.setField(bookmarkNew, "createdAt", LocalDateTime.now().minusDays(1));

                GetAllPartyBookmarkResponseDTO dtoA = GetAllPartyBookmarkResponseDTO.builder()
                        .partyId(11L).partyName("모임A").build();
                GetAllPartyBookmarkResponseDTO dtoB = GetAllPartyBookmarkResponseDTO.builder()
                        .partyId(12L).partyName("모임B").build();

                given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));
                given(partyBookmarkRepository.findAllByMemberWithParty(member))
                        .willReturn(new ArrayList<>(List.of(bookmarkOld, bookmarkNew)));
                given(bookmarkConverter.partyBookmarkToDTO(eq(bookmarkNew), any(), any(), any()))
                        .willReturn(dtoB);
                given(bookmarkConverter.partyBookmarkToDTO(eq(bookmarkOld), any(), any(), any()))
                        .willReturn(dtoA);

                // when
                List<GetAllPartyBookmarkResponseDTO> result =
                        bookmarkQueryService.getAllPartyBookmarks(member.getId(), PartyOrderType.LATEST);

                // then
                assertThat(result).hasSize(2);
                assertThat(result.get(0).partyId()).isEqualTo(12L); // 최신 것이 먼저
                assertThat(result.get(1).partyId()).isEqualTo(11L);
            }

            @Test
            @DisplayName("EXERCISE_COUNT 정렬 시 운동 횟수 많은 순으로 반환한다")
            void exerciseCountOrder_returnsMostExercisedFirst() {
                // given
                Party partyLow = PartyFixture.createParty("운동 적은 모임", member.getId(),
                        PartyFixture.createPartyAddr("서울특별시", "강남구"));
                ReflectionTestUtils.setField(partyLow, "id", 11L);
                ReflectionTestUtils.setField(partyLow, "exerciseCount", 2);

                Party partyHigh = PartyFixture.createParty("운동 많은 모임", member.getId(),
                        PartyFixture.createPartyAddr("서울특별시", "종로구"));
                ReflectionTestUtils.setField(partyHigh, "id", 12L);
                ReflectionTestUtils.setField(partyHigh, "exerciseCount", 10);

                PartyBookmark bookmarkLow = PartyBookmark.builder()
                        .member(member).party(partyLow)
                        .orderType(umc.cockple.demo.domain.party.enums.PartyOrderType.EXERCISE_COUNT).build();
                ReflectionTestUtils.setField(bookmarkLow, "createdAt", LocalDateTime.now().minusDays(1));

                PartyBookmark bookmarkHigh = PartyBookmark.builder()
                        .member(member).party(partyHigh)
                        .orderType(umc.cockple.demo.domain.party.enums.PartyOrderType.EXERCISE_COUNT).build();
                ReflectionTestUtils.setField(bookmarkHigh, "createdAt", LocalDateTime.now().minusDays(2));

                GetAllPartyBookmarkResponseDTO dtoLow = GetAllPartyBookmarkResponseDTO.builder()
                        .partyId(11L).partyName("운동 적은 모임").exerciseCnt(2).build();
                GetAllPartyBookmarkResponseDTO dtoHigh = GetAllPartyBookmarkResponseDTO.builder()
                        .partyId(12L).partyName("운동 많은 모임").exerciseCnt(10).build();

                given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));
                given(partyBookmarkRepository.findAllByMemberWithParty(member))
                        .willReturn(new ArrayList<>(List.of(bookmarkLow, bookmarkHigh)));
                given(bookmarkConverter.partyBookmarkToDTO(eq(bookmarkHigh), any(), any(), any()))
                        .willReturn(dtoHigh);
                given(bookmarkConverter.partyBookmarkToDTO(eq(bookmarkLow), any(), any(), any()))
                        .willReturn(dtoLow);

                // when
                List<GetAllPartyBookmarkResponseDTO> result =
                        bookmarkQueryService.getAllPartyBookmarks(member.getId(), PartyOrderType.EXERCISE_COUNT);

                // then
                assertThat(result).hasSize(2);
                assertThat(result.get(0).partyId()).isEqualTo(12L); // 운동 많은 모임이 먼저
                assertThat(result.get(1).partyId()).isEqualTo(11L);
            }

            @Test
            @DisplayName("파티에 미래 운동이 있을 때 가장 가까운 운동 정보를 함께 반환한다")
            void partyWithFutureExercise_returnsLatestExerciseInfo() {
                // given
                Exercise futureExercise = ExerciseFixture.createExercise(party,
                        LocalDate.now().plusDays(7), LocalTime.of(10, 0), true, false);
                party.addExercise(futureExercise);

                PartyBookmark bookmark = PartyBookmark.builder()
                        .member(member).party(party)
                        .orderType(umc.cockple.demo.domain.party.enums.PartyOrderType.LATEST).build();
                ReflectionTestUtils.setField(bookmark, "createdAt", LocalDateTime.now());

                GetAllPartyBookmarkResponseDTO dto = GetAllPartyBookmarkResponseDTO.builder()
                        .partyId(party.getId())
                        .latestExerciseDate(LocalDate.now().plusDays(7))
                        .latestExerciseTime(ActivityTime.MORNING)
                        .build();

                given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));
                given(partyBookmarkRepository.findAllByMemberWithParty(member))
                        .willReturn(new ArrayList<>(List.of(bookmark)));
                given(bookmarkConverter.partyBookmarkToDTO(eq(bookmark),
                        eq(futureExercise), eq(ActivityTime.MORNING), any()))
                        .willReturn(dto);

                // when
                List<GetAllPartyBookmarkResponseDTO> result =
                        bookmarkQueryService.getAllPartyBookmarks(member.getId(), PartyOrderType.LATEST);

                // then
                assertThat(result).hasSize(1);
                assertThat(result.get(0).latestExerciseDate()).isEqualTo(LocalDate.now().plusDays(7));
                assertThat(result.get(0).latestExerciseTime()).isEqualTo(ActivityTime.MORNING);
            }

            @Test
            @DisplayName("파티에 미래 운동이 없을 때 exercise는 null로 변환된다")
            void partyWithNoFutureExercise_passesNullExercise() {
                // given - 과거 운동만 있는 파티
                Exercise pastExercise = ExerciseFixture.createExercise(party,
                        LocalDate.now().minusDays(1), LocalTime.of(10, 0), true, false);
                party.addExercise(pastExercise);

                PartyBookmark bookmark = PartyBookmark.builder()
                        .member(member).party(party)
                        .orderType(umc.cockple.demo.domain.party.enums.PartyOrderType.LATEST).build();
                ReflectionTestUtils.setField(bookmark, "createdAt", LocalDateTime.now());

                GetAllPartyBookmarkResponseDTO dto = GetAllPartyBookmarkResponseDTO.builder()
                        .partyId(party.getId())
                        .latestExerciseDate(null)
                        .latestExerciseTime(null)
                        .build();

                given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));
                given(partyBookmarkRepository.findAllByMemberWithParty(member))
                        .willReturn(new ArrayList<>(List.of(bookmark)));
                given(bookmarkConverter.partyBookmarkToDTO(eq(bookmark), eq(null), eq(null), any()))
                        .willReturn(dto);

                // when
                List<GetAllPartyBookmarkResponseDTO> result =
                        bookmarkQueryService.getAllPartyBookmarks(member.getId(), PartyOrderType.LATEST);

                // then
                assertThat(result).hasSize(1);
                // null exercise 로 converter가 호출되었는지 검증
                verify(bookmarkConverter, times(1))
                        .partyBookmarkToDTO(eq(bookmark), eq(null), eq(null), any());
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("존재하지 않는 회원이면 MemberException(MEMBER_NOT_FOUND)을 던진다")
            void memberNotFound_throwsMemberException() {
                given(memberRepository.findById(999L)).willReturn(Optional.empty());

                assertThatThrownBy(() ->
                        bookmarkQueryService.getAllPartyBookmarks(999L, PartyOrderType.LATEST))
                        .isInstanceOf(MemberException.class)
                        .satisfies(e -> assertThat(((MemberException) e).getCode())
                                .isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND));
            }
        }
    }
}
