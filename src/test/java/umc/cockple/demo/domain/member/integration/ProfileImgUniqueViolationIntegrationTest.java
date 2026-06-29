package umc.cockple.demo.domain.member.integration;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.ProfileImg;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.IntegrationTestBase;
import umc.cockple.demo.support.fixture.MemberFixture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * MemberProfileUpdateExecutor 가 동시성 충돌을 식별하는 근거(제약명)가 실제 DB 에서 유효한지 보장하는 회귀 테스트.
 * 제약 이름이 바뀌거나 노출 형식이 달라지면(예: 테이블 접두사 제거) executor 의 부분 매칭과 함께 여기서 깨진다.
 */
class ProfileImgUniqueViolationIntegrationTest extends IntegrationTestBase {

    @Autowired MemberRepository memberRepository;
    @Autowired PlatformTransactionManager transactionManager;
    @PersistenceContext EntityManager em;

    private Long memberId;

    @AfterEach
    void tearDown() {
        if (memberId != null) {
            memberRepository.deleteById(memberId);
        }
    }

    @Test
    @DisplayName("같은 member 에 profile_img 를 2개 INSERT 하면 uq_profile_img_member 제약 위반이 cause 체인에 노출된다")
    void duplicate_profile_img_exposes_constraint_name() {
        Member saved = memberRepository.save(
                MemberFixture.createMember("진단", Gender.MALE, Level.A, 777777L));
        memberId = saved.getId();

        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        Throwable thrown = catchThrowable(() ->
                tx.executeWithoutResult(status -> {
                    Member m = memberRepository.findById(memberId).orElseThrow();
                    ProfileImg p1 = ProfileImg.builder().imgKey("a").build();
                    p1.setMember(m);
                    em.persist(p1);
                    ProfileImg p2 = ProfileImg.builder().imgKey("b").build();
                    p2.setMember(m);
                    em.persist(p2);
                })
        );

        assertThat(thrown).isNotNull();
        assertThat(constraintNameInChain(thrown))
                .as("executor 가 부분 매칭하는 제약명이 cause 체인에 존재해야 한다")
                .containsIgnoringCase("uq_profile_img_member");
    }

    private String constraintNameInChain(Throwable e) {
        for (Throwable t = e; t != null && t != t.getCause(); t = t.getCause()) {
            if (t instanceof ConstraintViolationException cve && cve.getConstraintName() != null) {
                return cve.getConstraintName();
            }
        }
        return "";
    }
}
