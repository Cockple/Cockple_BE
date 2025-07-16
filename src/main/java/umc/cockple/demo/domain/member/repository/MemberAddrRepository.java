package umc.cockple.demo.domain.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.cockple.demo.domain.member.domain.MemberAddr;

public interface MemberAddrRepository extends JpaRepository<MemberAddr, Long> {
}
