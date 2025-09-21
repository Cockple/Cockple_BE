package umc.cockple.demo.domain.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberAddr;

import java.util.Optional;

public interface MemberAddrRepository extends JpaRepository<MemberAddr, Long> {
    Optional<MemberAddr> findByMemberAndIsMain(Member member, Boolean isMain);
}
