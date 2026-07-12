package umc.cockple.demo.domain.member.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.member.dto.MemberDetailInfoRequestDTO;
import umc.cockple.demo.domain.member.dto.UpdateProfileRequestDTO;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.exception.MemberException;

/**
 * 프로필을 쓰는 작업(온보딩 등록 / 프로필 수정)의 동시성 충돌을 트랜잭션 밖에서 재시도해 멱등하게 만든다.
 *
 * 신규 등록 동시 진입 -> 패배 트랜잭션이 unique 제약 위반
 * 기존 사진 교체 동시 진입 -> 패배 트랜잭션이 낙관적 락 실패
 *
 * 두 경우 모두 재시도하면 프로필이 이미 존재/최신 버전이므로 UPDATE 경로로 수렴해 성공한다.
 * (재시도가 트랜잭션 경계 밖에서 일어나야 매번 새 트랜잭션/영속성 컨텍스트가 열린다.)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MemberProfileUpdateExecutor {

    private static final int MAX_ATTEMPTS = 3;

    private static final String PROFILE_IMG_UNIQUE_CONSTRAINT = "uq_profile_img_member";

    private final MemberCommandService memberCommandService;

    public void registerMemberDetailInfo(Long memberId, MemberDetailInfoRequestDTO requestDto) {
        runWithConflictRetry(memberId, () -> memberCommandService.memberDetailInfo(memberId, requestDto));
    }

    public void updateProfile(UpdateProfileRequestDTO requestDto, Long memberId) {
        runWithConflictRetry(memberId, () -> memberCommandService.updateProfile(requestDto, memberId));
    }

    private void runWithConflictRetry(Long memberId, Runnable operation) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                operation.run();
                return;
            } catch (ObjectOptimisticLockingFailureException e) {
                // 낙관적 락 실패 = 프로필 사진 동시 교체 (ProfileImg 만 @Version 보유) -> 재시도
                log.warn("프로필 동시성 충돌(낙관적 락) - memberId: {}, 시도: {}/{}", memberId, attempt, MAX_ATTEMPTS);
            } catch (DataIntegrityViolationException e) {
                // 무결성 위반 중 '프로필 이미지 유니크' 만 동시성 충돌로 간주, 그 외(NOT NULL/FK 등)는 진짜 에러로 전파
                if (!isProfileImageUniqueViolation(e)) {
                    throw e;
                }
                log.warn("프로필 동시성 충돌 - memberId: {}, 시도: {}/{}", memberId, attempt, MAX_ATTEMPTS);
            }
        }
        throw new MemberException(MemberErrorCode.PROFILE_UPDATE_CONFLICT);
    }


    private boolean isProfileImageUniqueViolation(Throwable e) {
        for (Throwable t = e; t != null && t != t.getCause(); t = t.getCause()) {
            if (t instanceof ConstraintViolationException cve) {
                String name = cve.getConstraintName();
                if (name != null && name.toLowerCase().contains(PROFILE_IMG_UNIQUE_CONSTRAINT)) {
                    return true;
                }
            }
            String message = t.getMessage();
            if (message != null && message.toLowerCase().contains(PROFILE_IMG_UNIQUE_CONSTRAINT)) {
                return true;
            }
        }
        return false;
    }
}
