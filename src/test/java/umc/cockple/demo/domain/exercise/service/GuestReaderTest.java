package umc.cockple.demo.domain.exercise.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.cockple.demo.domain.exercise.domain.Guest;
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.exception.ExerciseException;
import umc.cockple.demo.domain.exercise.repository.GuestRepository;
import umc.cockple.demo.domain.exercise.service.support.reader.GuestReader;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("GuestReader")
class GuestReaderTest {

    @InjectMocks
    private GuestReader guestReader;

    @Mock private GuestRepository guestRepository;
    @Mock private Guest guest;

    @Test
    @DisplayName("게스트 ID로 참가 취소 대상 게스트를 조회한다")
    void findByIdOrThrow_returnsGuest() {
        given(guestRepository.findById(1L)).willReturn(Optional.of(guest));

        assertThat(guestReader.findByIdOrThrow(1L)).isSameAs(guest);
    }

    @Test
    @DisplayName("게스트가 없으면 ExerciseException(GUEST_NOT_FOUND)을 던진다")
    void findByIdOrThrow_throwsWhenMissing() {
        given(guestRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> guestReader.findByIdOrThrow(1L))
                .isInstanceOf(ExerciseException.class)
                .satisfies(exception -> assertThat(((ExerciseException) exception).getCode())
                        .isEqualTo(ExerciseErrorCode.GUEST_NOT_FOUND));
    }
}
