package umc.cockple.demo.domain.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberTerms;

public interface MemberTermsRepository extends JpaRepository<MemberTerms, Long> {
    void deleteByMember(Member member);
}
