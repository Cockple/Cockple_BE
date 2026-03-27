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
import umc.cockple.demo.domain.bookmark.domain.ExerciseBookmark;
import umc.cockple.demo.domain.bookmark.domain.PartyBookmark;
import umc.cockple.demo.domain.bookmark.exception.BookmarkErrorCode;
import umc.cockple.demo.domain.bookmark.exception.BookmarkException;
import umc.cockple.demo.domain.bookmark.repository.ExerciseBookmarkRepository;
import umc.cockple.demo.domain.bookmark.repository.PartyBookmarkRepository;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.exception.ExerciseException;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.exception.MemberException;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.enums.PartyOrderType;
import umc.cockple.demo.domain.party.enums.PartyStatus;
import umc.cockple.demo.domain.party.exception.PartyErrorCode;
import umc.cockple.demo.domain.party.exception.PartyException;
import umc.cockple.demo.domain.party.repository.PartyRepository;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.fixture.ExerciseFixture;
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookmarkCommandService")
class BookmarkCommandServiceTest {

    @InjectMocks
    private BookmarkCommandService bookmarkCommandService;

    @Mock private PartyBookmarkRepository partyBookmarkRepository;
    @Mock private ExerciseBookmarkRepository exerciseBookmarkRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private PartyRepository partyRepository;
    @Mock private ExerciseRepository exerciseRepository;

    private Member member;
    private Party party;
    private Exercise exercise;

    @BeforeEach
    void setUp() {
        member = MemberFixture.createMember("테스트 유저", Gender.MALE, Level.A, 1001L);
        ReflectionTestUtils.setField(member, "id", 1L);

        party = PartyFixture.createParty("테스트 모임", member.getId(),
                PartyFixture.createPartyAddr("서울특별시", "강남구"));
        ReflectionTestUtils.setField(party, "id", 10L);

        exercise = ExerciseFixture.createExercise(party, LocalDate.of(2099, 12, 31));
        ReflectionTestUtils.setField(exercise, "id", 100L);
    }

    @Nested
    @DisplayName("partyBookmark - 모임 찜하기")
    class PartyBookmarks {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("모임을 찜하면 저장된 북마크 id를 반환한다")
            void createPartyBookmark_returnsBookmarkId() {
                // given
                PartyBookmark savedBookmark = PartyBookmark.builder()
                        .member(member)
                        .party(party)
                        .orderType(PartyOrderType.LATEST)
                        .build();
                ReflectionTestUtils.setField(savedBookmark, "id", 50L);

                given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));
                given(partyRepository.findById(party.getId())).willReturn(Optional.of(party));
                given(partyBookmarkRepository.existsByMemberAndParty(member, party)).willReturn(false);
                given(partyBookmarkRepository.findAllByMember(member)).willReturn(new ArrayList<>());
                given(partyBookmarkRepository.save(any(PartyBookmark.class))).willReturn(savedBookmark);

                // when
                Long result = bookmarkCommandService.partyBookmark(member.getId(), party.getId());

                // then
                assertThat(result).isEqualTo(50L);
                then(partyBookmarkRepository).should().save(any(PartyBookmark.class));
            }

            @Test
            @DisplayName("찜 목록이 15개 이상이면 가장 오래된 북마크를 삭제하고 새로 저장한다")
            void createPartyBookmark_deletesOldestWhenLimitExceeded() {
                // given
                List<PartyBookmark> existingBookmarks = new ArrayList<>();
                for (int i = 0; i < 15; i++) {
                    existingBookmarks.add(PartyBookmark.builder()
                            .member(member)
                            .party(party)
                            .orderType(PartyOrderType.LATEST)
                            .build());
                }

                PartyBookmark oldestBookmark = PartyBookmark.builder()
                        .member(member)
                        .party(party)
                        .orderType(PartyOrderType.LATEST)
                        .build();

                PartyBookmark savedBookmark = PartyBookmark.builder()
                        .member(member)
                        .party(party)
                        .orderType(PartyOrderType.LATEST)
                        .build();
                ReflectionTestUtils.setField(savedBookmark, "id", 99L);

                given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));
                given(partyRepository.findById(party.getId())).willReturn(Optional.of(party));
                given(partyBookmarkRepository.existsByMemberAndParty(member, party)).willReturn(false);
                given(partyBookmarkRepository.findAllByMember(member)).willReturn(existingBookmarks);
                given(partyBookmarkRepository.findFirstByMemberOrderByCreatedAtAsc(member))
                        .willReturn(Optional.of(oldestBookmark));
                given(partyBookmarkRepository.save(any(PartyBookmark.class))).willReturn(savedBookmark);

                // when
                Long result = bookmarkCommandService.partyBookmark(member.getId(), party.getId());

                // then
                assertThat(result).isEqualTo(99L);
                then(partyBookmarkRepository).should().delete(oldestBookmark);
                then(partyBookmarkRepository).should().save(any(PartyBookmark.class));
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
                        bookmarkCommandService.partyBookmark(999L, party.getId()))
                        .isInstanceOf(MemberException.class)
                        .satisfies(e -> assertThat(((MemberException) e).getCode())
                                .isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND));
            }

            @Test
            @DisplayName("존재하지 않는 모임이면 PartyException(PARTY_NOT_FOUND)을 던진다")
            void partyNotFound_throwsPartyException() {
                given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));
                given(partyRepository.findById(999L)).willReturn(Optional.empty());

                assertThatThrownBy(() ->
                        bookmarkCommandService.partyBookmark(member.getId(), 999L))
                        .isInstanceOf(PartyException.class)
                        .satisfies(e -> assertThat(((PartyException) e).getCode())
                                .isEqualTo(PartyErrorCode.PARTY_NOT_FOUND));
            }

            @Test
            @DisplayName("비활성화된 모임이면 PartyException(PARTY_IS_DELETED)을 던진다")
            void partyIsInactive_throwsPartyException() {
                Party inactiveParty = PartyFixture.createParty("삭제된 모임", member.getId(),
                        PartyFixture.createPartyAddr("서울특별시", "강남구"));
                ReflectionTestUtils.setField(inactiveParty, "id", 20L);
                ReflectionTestUtils.setField(inactiveParty, "status", PartyStatus.INACTIVE);

                given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));
                given(partyRepository.findById(20L)).willReturn(Optional.of(inactiveParty));

                assertThatThrownBy(() ->
                        bookmarkCommandService.partyBookmark(member.getId(), 20L))
                        .isInstanceOf(PartyException.class)
                        .satisfies(e -> assertThat(((PartyException) e).getCode())
                                .isEqualTo(PartyErrorCode.PARTY_IS_DELETED));
            }

            @Test
            @DisplayName("이미 찜한 모임이면 BookmarkException(ALREADY_BOOKMARK)을 던진다")
            void alreadyBookmarked_throwsBookmarkException() {
                given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));
                given(partyRepository.findById(party.getId())).willReturn(Optional.of(party));
                given(partyBookmarkRepository.existsByMemberAndParty(member, party)).willReturn(true);

                assertThatThrownBy(() ->
                        bookmarkCommandService.partyBookmark(member.getId(), party.getId()))
                        .isInstanceOf(BookmarkException.class)
                        .satisfies(e -> assertThat(((BookmarkException) e).getCode())
                                .isEqualTo(BookmarkErrorCode.ALREADY_BOOKMARK));

                then(partyBookmarkRepository).should(never()).save(any());
            }
        }
    }

    @Nested
    @DisplayName("releasePartyBookmark - 모임 찜 해제")
    class ReleasePartyBookmarks {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("찜한 모임을 해제하면 북마크를 삭제한다")
            void releasePartyBookmark_deletesBookmark() {
                // given
                PartyBookmark bookmark = PartyBookmark.builder()
                        .member(member)
                        .party(party)
                        .orderType(PartyOrderType.LATEST)
                        .build();

                given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));
                given(partyRepository.findById(party.getId())).willReturn(Optional.of(party));
                given(partyBookmarkRepository.findByMemberAndParty(member, party))
                        .willReturn(Optional.of(bookmark));

                // when
                bookmarkCommandService.releasePartyBookmark(member.getId(), party.getId());

                // then
                then(partyBookmarkRepository).should().delete(bookmark);
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
                        bookmarkCommandService.releasePartyBookmark(999L, party.getId()))
                        .isInstanceOf(MemberException.class)
                        .satisfies(e -> assertThat(((MemberException) e).getCode())
                                .isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND));
            }

            @Test
            @DisplayName("존재하지 않는 모임이면 PartyException(PARTY_NOT_FOUND)을 던진다")
            void partyNotFound_throwsPartyException() {
                given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));
                given(partyRepository.findById(999L)).willReturn(Optional.empty());

                assertThatThrownBy(() ->
                        bookmarkCommandService.releasePartyBookmark(member.getId(), 999L))
                        .isInstanceOf(PartyException.class)
                        .satisfies(e -> assertThat(((PartyException) e).getCode())
                                .isEqualTo(PartyErrorCode.PARTY_NOT_FOUND));
            }

            @Test
            @DisplayName("찜하지 않은 모임이면 BookmarkException(ALREADY_RELEASE_BOOKMARK)을 던진다")
            void bookmarkNotFound_throwsBookmarkException() {
                given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));
                given(partyRepository.findById(party.getId())).willReturn(Optional.of(party));
                given(partyBookmarkRepository.findByMemberAndParty(member, party))
                        .willReturn(Optional.empty());

                assertThatThrownBy(() ->
                        bookmarkCommandService.releasePartyBookmark(member.getId(), party.getId()))
                        .isInstanceOf(BookmarkException.class)
                        .satisfies(e -> assertThat(((BookmarkException) e).getCode())
                                .isEqualTo(BookmarkErrorCode.ALREADY_RELEASE_BOOKMARK));

                then(partyBookmarkRepository).should(never()).delete(any());
            }
        }
    }

    @Nested
    @DisplayName("exerciseBookmark - 운동 찜하기")
    class ExerciseBookmarkCreate {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("운동을 찜하면 저장된 북마크 id를 반환한다")
            void createExerciseBookmark_returnsBookmarkId() {
                // given
                ExerciseBookmark savedBookmark = ExerciseBookmark.builder()
                        .member(member)
                        .exercise(exercise)
                        .build();
                ReflectionTestUtils.setField(savedBookmark, "id", 200L);

                given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));
                given(exerciseRepository.findById(exercise.getId())).willReturn(Optional.of(exercise));
                given(exerciseBookmarkRepository.existsByMemberAndExercise(member, exercise)).willReturn(false);
                given(exerciseBookmarkRepository.findAllByMember(member)).willReturn(new ArrayList<>());
                given(exerciseBookmarkRepository.save(any(ExerciseBookmark.class))).willReturn(savedBookmark);

                // when
                Long result = bookmarkCommandService.exerciseBookmark(member.getId(), exercise.getId());

                // then
                assertThat(result).isEqualTo(200L);
                then(exerciseBookmarkRepository).should().save(any(ExerciseBookmark.class));
            }

            @Test
            @DisplayName("찜 목록이 50개 이상이면 가장 오래된 북마크를 삭제하고 새로 저장한다")
            void createExerciseBookmark_deletesOldestWhenLimitExceeded() {
                // given
                List<ExerciseBookmark> existingBookmarks = new ArrayList<>();
                for (int i = 0; i < 50; i++) {
                    existingBookmarks.add(ExerciseBookmark.builder()
                            .member(member)
                            .exercise(exercise)
                            .build());
                }

                ExerciseBookmark oldestBookmark = ExerciseBookmark.builder()
                        .member(member)
                        .exercise(exercise)
                        .build();

                ExerciseBookmark savedBookmark = ExerciseBookmark.builder()
                        .member(member)
                        .exercise(exercise)
                        .build();
                ReflectionTestUtils.setField(savedBookmark, "id", 300L);

                given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));
                given(exerciseRepository.findById(exercise.getId())).willReturn(Optional.of(exercise));
                given(exerciseBookmarkRepository.existsByMemberAndExercise(member, exercise)).willReturn(false);
                given(exerciseBookmarkRepository.findAllByMember(member)).willReturn(existingBookmarks);
                given(exerciseBookmarkRepository.findFirstByMemberOrderByCreatedAtAsc(member))
                        .willReturn(Optional.of(oldestBookmark));
                given(exerciseBookmarkRepository.save(any(ExerciseBookmark.class))).willReturn(savedBookmark);

                // when
                Long result = bookmarkCommandService.exerciseBookmark(member.getId(), exercise.getId());

                // then
                assertThat(result).isEqualTo(300L);
                then(exerciseBookmarkRepository).should().delete(oldestBookmark);
                then(exerciseBookmarkRepository).should().save(any(ExerciseBookmark.class));
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
                        bookmarkCommandService.exerciseBookmark(999L, exercise.getId()))
                        .isInstanceOf(MemberException.class)
                        .satisfies(e -> assertThat(((MemberException) e).getCode())
                                .isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND));
            }

            @Test
            @DisplayName("존재하지 않는 운동이면 ExerciseException(EXERCISE_NOT_FOUND)을 던진다")
            void exerciseNotFound_throwsExerciseException() {
                given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));
                given(exerciseRepository.findById(999L)).willReturn(Optional.empty());

                assertThatThrownBy(() ->
                        bookmarkCommandService.exerciseBookmark(member.getId(), 999L))
                        .isInstanceOf(ExerciseException.class)
                        .satisfies(e -> assertThat(((ExerciseException) e).getCode())
                                .isEqualTo(ExerciseErrorCode.EXERCISE_NOT_FOUND));
            }

            @Test
            @DisplayName("이미 찜한 운동이면 BookmarkException(ALREADY_BOOKMARK)을 던진다")
            void alreadyBookmarked_throwsBookmarkException() {
                given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));
                given(exerciseRepository.findById(exercise.getId())).willReturn(Optional.of(exercise));
                given(exerciseBookmarkRepository.existsByMemberAndExercise(member, exercise)).willReturn(true);

                assertThatThrownBy(() ->
                        bookmarkCommandService.exerciseBookmark(member.getId(), exercise.getId()))
                        .isInstanceOf(BookmarkException.class)
                        .satisfies(e -> assertThat(((BookmarkException) e).getCode())
                                .isEqualTo(BookmarkErrorCode.ALREADY_BOOKMARK));

                then(exerciseBookmarkRepository).should(never()).save(any());
            }
        }
    }

    @Nested
    @DisplayName("releaseExerciseBookmark - 운동 찜 해제")
    class ReleaseExerciseBookmark {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("찜한 운동을 해제하면 북마크를 삭제한다")
            void releaseExerciseBookmark_deletesBookmark() {
                // given
                ExerciseBookmark bookmark = ExerciseBookmark.builder()
                        .member(member)
                        .exercise(exercise)
                        .build();

                given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));
                given(exerciseRepository.findById(exercise.getId())).willReturn(Optional.of(exercise));
                given(exerciseBookmarkRepository.findByMemberAndExercise(member, exercise))
                        .willReturn(Optional.of(bookmark));

                // when
                bookmarkCommandService.releaseExerciseBookmark(member.getId(), exercise.getId());

                // then
                then(exerciseBookmarkRepository).should().delete(bookmark);
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
                        bookmarkCommandService.releaseExerciseBookmark(999L, exercise.getId()))
                        .isInstanceOf(MemberException.class)
                        .satisfies(e -> assertThat(((MemberException) e).getCode())
                                .isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND));
            }

            @Test
            @DisplayName("존재하지 않는 운동이면 ExerciseException(EXERCISE_NOT_FOUND)을 던진다")
            void exerciseNotFound_throwsExerciseException() {
                given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));
                given(exerciseRepository.findById(999L)).willReturn(Optional.empty());

                assertThatThrownBy(() ->
                        bookmarkCommandService.releaseExerciseBookmark(member.getId(), 999L))
                        .isInstanceOf(ExerciseException.class)
                        .satisfies(e -> assertThat(((ExerciseException) e).getCode())
                                .isEqualTo(ExerciseErrorCode.EXERCISE_NOT_FOUND));
            }

            @Test
            @DisplayName("찜하지 않은 운동이면 BookmarkException(ALREADY_RELEASE_BOOKMARK)을 던진다")
            void bookmarkNotFound_throwsBookmarkException() {
                given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));
                given(exerciseRepository.findById(exercise.getId())).willReturn(Optional.of(exercise));
                given(exerciseBookmarkRepository.findByMemberAndExercise(member, exercise))
                        .willReturn(Optional.empty());

                assertThatThrownBy(() ->
                        bookmarkCommandService.releaseExerciseBookmark(member.getId(), exercise.getId()))
                        .isInstanceOf(BookmarkException.class)
                        .satisfies(e -> assertThat(((BookmarkException) e).getCode())
                                .isEqualTo(BookmarkErrorCode.ALREADY_RELEASE_BOOKMARK));

                then(exerciseBookmarkRepository).should(never()).delete(any());
            }
        }
    }
}
