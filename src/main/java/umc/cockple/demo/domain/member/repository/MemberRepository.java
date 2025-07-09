package umc.cockple.demo.domain.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.cockple.demo.domain.member.domain.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {
}
