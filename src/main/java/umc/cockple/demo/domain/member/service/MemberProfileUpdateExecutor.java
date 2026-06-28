package umc.cockple.demo.domain.member.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.member.dto.UpdateProfileRequestDTO;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.exception.MemberException;

/**
 * 프로필 수정의 동시성 충돌을 트랜잭션 밖에서 재시도해 멱등하게 만든다.
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

    private final MemberCommandService memberCommandService;

    public void updateProfile(UpdateProfileRequestDTO requestDto, Long memberId) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                memberCommandService.updateProfile(requestDto, memberId);
                return;
            } catch (DataIntegrityViolationException | ObjectOptimisticLockingFailureException e) {
                log.warn("프로필 수정 동시성 충돌 감지 - memberId: {}, 시도: {}/{}", memberId, attempt, MAX_ATTEMPTS);
            }
        }
        throw new MemberException(MemberErrorCode.PROFILE_UPDATE_CONFLICT);
    }
}
