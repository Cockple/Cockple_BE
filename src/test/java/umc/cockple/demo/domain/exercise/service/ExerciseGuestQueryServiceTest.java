package umc.cockple.demo.domain.exercise.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;
import umc.cockple.demo.domain.bookmark.repository.ExerciseBookmarkRepository;
import umc.cockple.demo.domain.exercise.converter.ExerciseConverter;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.domain.Guest;
import umc.cockple.demo.domain.exercise.dto.ExerciseBuildingDetailDTO;
import umc.cockple.demo.domain.exercise.dto.ExerciseDetailDTO;
import umc.cockple.demo.domain.exercise.dto.ExerciseEditDetailDTO;
import umc.cockple.demo.domain.exercise.dto.ExerciseMapBuildingsDTO;
import umc.cockple.demo.domain.exercise.dto.ExerciseMyGuestListDTO;
import umc.cockple.demo.domain.exercise.dto.MyExerciseCalendarDTO;
import umc.cockple.demo.domain.exercise.dto.MyExerciseListDTO;
import umc.cockple.demo.domain.exercise.dto.MyPartyExerciseCalendarDTO;
import umc.cockple.demo.domain.exercise.dto.MyPartyExerciseDTO;
import umc.cockple.demo.domain.exercise.dto.PartyExerciseCalendarDTO;
import umc.cockple.demo.domain.exercise.enums.MyExerciseFilterType;
import umc.cockple.demo.domain.exercise.enums.MyExerciseOrderType;
import umc.cockple.demo.domain.exercise.enums.MyPartyExerciseOrderType;
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.exception.ExerciseException;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.exercise.repository.GuestRepository;
import umc.cockple.demo.domain.exercise.service.support.ExerciseParticipantInfoAssembler;
import umc.cockple.demo.domain.exercise.service.support.reader.ExerciseParticipantReader;
import umc.cockple.demo.domain.exercise.service.support.reader.ExerciseReader;
import umc.cockple.demo.domain.exercise.service.support.reader.GuestReader;
import umc.cockple.demo.domain.exercise.service.query.ExerciseGuestQueryService;
import umc.cockple.demo.domain.member.service.support.MemberLookupService;
import umc.cockple.demo.domain.party.service.support.PartyLookupService;
import umc.cockple.demo.domain.file.service.FileService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberExercise;
import umc.cockple.demo.domain.member.domain.MemberParty;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.exception.MemberException;
import umc.cockple.demo.domain.member.repository.MemberExerciseRepository;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.enums.PartyStatus;
import umc.cockple.demo.domain.party.exception.PartyErrorCode;
import umc.cockple.demo.domain.party.exception.PartyException;
import umc.cockple.demo.domain.party.repository.PartyRepository;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.global.enums.Role;
import umc.cockple.demo.support.ExerciseCalendarTestHelper;
import umc.cockple.demo.support.fixture.ExerciseFixture;
import umc.cockple.demo.support.fixture.GuestFixture;
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExerciseGuestQueryService")
class ExerciseGuestQueryServiceTest {

    private ExerciseGuestQueryService exerciseGuestQueryService;

    @Mock private ExerciseRepository exerciseRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private MemberPartyRepository memberPartyRepository;
    @Mock private MemberExerciseRepository memberExerciseRepository;
    @Mock private GuestRepository guestRepository;
    @Mock private FileService fileService;

    private Member manager;
    private Party party;
    private Exercise exercise;

    @BeforeEach
    void setUp() {
        ExerciseConverter exerciseConverter = new ExerciseConverter(fileService);
        ExerciseParticipantReader exerciseParticipantReader = new ExerciseParticipantReader(
                memberExerciseRepository, memberPartyRepository);
        GuestReader guestReader = new GuestReader(guestRepository);
        MemberLookupService memberLookupService = new MemberLookupService(memberRepository);

        exerciseGuestQueryService = new ExerciseGuestQueryService(
                new ExerciseReader(exerciseRepository),
                guestReader,
                new ExerciseParticipantInfoAssembler(
                        exerciseParticipantReader,
                        guestReader,
                        memberLookupService,
                        exerciseConverter
                ),
                memberLookupService,
                exerciseConverter
        );

        manager = MemberFixture.createMember("모임장", Gender.MALE, Level.A, 1001L);
        ReflectionTestUtils.setField(manager, "id", 1L);

        party = PartyFixture.createParty("테스트 모임", manager.getId(),
                PartyFixture.createPartyAddr("서울특별시", "강남구"));
        ReflectionTestUtils.setField(party, "id", 10L);

        exercise = ExerciseFixture.createExercise(party, LocalDate.now().minusDays(1));
        ReflectionTestUtils.setField(exercise, "id", 100L);
        ReflectionTestUtils.setField(exercise, "exerciseAddr", ExerciseFixture.createExerciseAddr());
    }

    @Nested
    @DisplayName("getMyInvitedGuests")
    class GetMyInvitedGuests {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("내가_초대한_게스트만_참가번호와_대기상태와_함께_반환된다")
            void 내가_초대한_게스트만_참가번호와_대기상태와_함께_반환된다() {
                // given
                ReflectionTestUtils.setField(exercise, "maxCapacity", 1);

                Guest myFirstGuest = GuestFixture.createGuest(exercise, manager.getId(), "내게스트1", Gender.MALE);
                ReflectionTestUtils.setField(myFirstGuest, "id", 201L);
                ReflectionTestUtils.setField(myFirstGuest, "createdAt", LocalDateTime.now().minusMinutes(3));

                Guest otherInvitedGuest = GuestFixture.createGuest(exercise, 2L, "다른사람게스트", Gender.MALE);
                ReflectionTestUtils.setField(otherInvitedGuest, "id", 202L);
                ReflectionTestUtils.setField(otherInvitedGuest, "createdAt", LocalDateTime.now().minusMinutes(2));

                Guest mySecondGuest = GuestFixture.createGuest(exercise, manager.getId(), "내게스트2", Gender.FEMALE);
                ReflectionTestUtils.setField(mySecondGuest, "id", 203L);
                ReflectionTestUtils.setField(mySecondGuest, "createdAt", LocalDateTime.now().minusMinutes(1));

                given(exerciseRepository.findExerciseWithBasicInfo(exercise.getId()))
                        .willReturn(Optional.of(exercise));
                given(memberRepository.findById(manager.getId()))
                        .willReturn(Optional.of(manager));
                given(guestRepository.findByExerciseIdAndInviterId(exercise.getId(), manager.getId()))
                        .willReturn(List.of(myFirstGuest, mySecondGuest));
                given(memberExerciseRepository.findByExerciseIdWithMemberAndProfile(exercise.getId()))
                        .willReturn(List.of());
                given(guestRepository.findByExerciseId(exercise.getId()))
                        .willReturn(List.of(myFirstGuest, otherInvitedGuest, mySecondGuest));

                // when
                ExerciseMyGuestListDTO.Response response = exerciseGuestQueryService.getMyInvitedGuests(
                        exercise.getId(), manager.getId());

                // then
                assertThat(response.totalCount()).isEqualTo(2);
                assertThat(response.maleCount()).isEqualTo(1);
                assertThat(response.femaleCount()).isEqualTo(1);
                assertThat(response.list())
                        .extracting(
                                ExerciseMyGuestListDTO.GuestInfo::guestId,
                                ExerciseMyGuestListDTO.GuestInfo::isWaiting,
                                ExerciseMyGuestListDTO.GuestInfo::participantNumber,
                                ExerciseMyGuestListDTO.GuestInfo::name,
                                ExerciseMyGuestListDTO.GuestInfo::gender,
                                ExerciseMyGuestListDTO.GuestInfo::level,
                                ExerciseMyGuestListDTO.GuestInfo::inviterName
                        )
                        .containsExactly(
                                tuple(201L, false, 1, "내게스트1", Gender.MALE, Level.B, manager.getMemberName()),
                                tuple(203L, true, 2, "내게스트2", Gender.FEMALE, Level.B, manager.getMemberName())
                        );
            }

            @Test
            @DisplayName("초대한_게스트가_없으면_빈_응답을_반환한다")
            void 초대한_게스트가_없으면_빈_응답을_반환한다() {
                // given
                given(exerciseRepository.findExerciseWithBasicInfo(exercise.getId()))
                        .willReturn(Optional.of(exercise));
                given(memberRepository.findById(manager.getId()))
                        .willReturn(Optional.of(manager));
                given(guestRepository.findByExerciseIdAndInviterId(exercise.getId(), manager.getId()))
                        .willReturn(List.of());

                // when
                ExerciseMyGuestListDTO.Response response = exerciseGuestQueryService.getMyInvitedGuests(
                        exercise.getId(), manager.getId());

                // then
                assertThat(response.totalCount()).isZero();
                assertThat(response.maleCount()).isZero();
                assertThat(response.femaleCount()).isZero();
                assertThat(response.list()).isEmpty();
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("존재하지_않는_운동이면_예외를_던진다")
            void 존재하지_않는_운동이면_예외를_던진다() {
                // given
                given(exerciseRepository.findExerciseWithBasicInfo(999L))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> exerciseGuestQueryService.getMyInvitedGuests(999L, manager.getId()))
                        .isInstanceOf(ExerciseException.class)
                        .hasFieldOrPropertyWithValue("code", ExerciseErrorCode.EXERCISE_NOT_FOUND);
            }

            @Test
            @DisplayName("존재하지_않는_멤버면_예외를_던진다")
            void 존재하지_않는_멤버면_예외를_던진다() {
                // given
                given(exerciseRepository.findExerciseWithBasicInfo(exercise.getId()))
                        .willReturn(Optional.of(exercise));
                given(memberRepository.findById(999L))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> exerciseGuestQueryService.getMyInvitedGuests(exercise.getId(), 999L))
                        .isInstanceOf(MemberException.class)
                        .hasFieldOrPropertyWithValue("code", MemberErrorCode.MEMBER_NOT_FOUND);
            }
        }
    }



}
