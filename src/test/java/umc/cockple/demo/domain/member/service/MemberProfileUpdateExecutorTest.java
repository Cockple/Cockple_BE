package umc.cockple.demo.domain.member.service;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.exception.MemberException;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberProfileUpdateExecutor")
class MemberProfileUpdateExecutorTest {

    @Mock
    private MemberCommandService memberCommandService;

    @InjectMocks
    private MemberProfileUpdateExecutor executor;

    /** profile_img 유니크 제약 위반 (MySQL8 형식: 테이블 접두사 포함). */
    private DataIntegrityViolationException profileUniqueConflict() {
        ConstraintViolationException cve = new ConstraintViolationException(
                "could not execute statement",
                new SQLException("Duplicate entry '1' for key 'profile_img.uq_profile_img_member'", "23000", 1062),
                "profile_img.uq_profile_img_member");
        return new DataIntegrityViolationException("duplicate", cve);
    }

    /** 프로필과 무관한 다른 무결성 위반 (예: 다른 제약). */
    private DataIntegrityViolationException otherConstraintConflict() {
        ConstraintViolationException cve = new ConstraintViolationException(
                "could not execute statement",
                new SQLException("Cannot add or update a child row", "23000", 1452),
                "fk_some_other_constraint");
        return new DataIntegrityViolationException("other", cve);
    }

    @Nested
    @DisplayName("updateProfile - 동시성 충돌 재시도")
    class UpdateProfileRetry {

        @Test
        @DisplayName("프로필 유니크 충돌이 한 번 나면 재시도해서 성공한다")
        void 유니크_충돌_후_재시도_성공() {
            doThrow(profileUniqueConflict()).doNothing()
                    .when(memberCommandService).updateProfile(any(), any());

            executor.updateProfile(null, 1L);

            verify(memberCommandService, times(2)).updateProfile(any(), any());
        }

        @Test
        @DisplayName("낙관적 락 충돌이 한 번 나면 재시도해서 성공한다")
        void 낙관적락_충돌_후_재시도_성공() {
            doThrow(new ObjectOptimisticLockingFailureException("stale", new RuntimeException()))
                    .doNothing()
                    .when(memberCommandService).updateProfile(any(), any());

            executor.updateProfile(null, 1L);

            verify(memberCommandService, times(2)).updateProfile(any(), any());
        }

        @Test
        @DisplayName("충돌이 최대 시도까지 계속되면 PROFILE_UPDATE_CONFLICT(409)를 던진다")
        void 충돌_지속시_409() {
            doThrow(profileUniqueConflict())
                    .when(memberCommandService).updateProfile(any(), any());

            Throwable thrown = catchThrowable(() -> executor.updateProfile(null, 1L));

            assertThat(thrown).isInstanceOfSatisfying(MemberException.class,
                    e -> assertThat(e.getCode()).isEqualTo(MemberErrorCode.PROFILE_UPDATE_CONFLICT));
            verify(memberCommandService, times(3)).updateProfile(any(), any());
        }

        @Test
        @DisplayName("프로필과 무관한 무결성 위반은 재시도하지 않고 원래 예외를 그대로 전파한다")
        void 다른_제약위반은_그대로_전파() {
            DataIntegrityViolationException other = otherConstraintConflict();
            doThrow(other).when(memberCommandService).updateProfile(any(), any());

            Throwable thrown = catchThrowable(() -> executor.updateProfile(null, 1L));

            assertThat(thrown)
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .isNotInstanceOf(MemberException.class);
            verify(memberCommandService, times(1)).updateProfile(any(), any());
        }
    }

    @Nested
    @DisplayName("registerMemberDetailInfo - 온보딩도 동일한 재시도 보호를 받는다")
    class OnboardingRetry {

        @Test
        @DisplayName("프로필 유니크 충돌이 한 번 나면 재시도해서 성공한다")
        void 온보딩_유니크_충돌_후_재시도_성공() {
            doThrow(profileUniqueConflict()).doNothing()
                    .when(memberCommandService).memberDetailInfo(any(), any());

            executor.registerMemberDetailInfo(1L, null);

            verify(memberCommandService, times(2)).memberDetailInfo(any(), any());
        }

        @Test
        @DisplayName("프로필과 무관한 무결성 위반은 그대로 전파한다")
        void 온보딩_다른_제약위반은_전파() {
            doThrow(otherConstraintConflict()).when(memberCommandService).memberDetailInfo(any(), any());

            assertThatThrownBy(() -> executor.registerMemberDetailInfo(1L, null))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .isNotInstanceOf(MemberException.class);
            verify(memberCommandService, times(1)).memberDetailInfo(any(), any());
        }
    }
}
