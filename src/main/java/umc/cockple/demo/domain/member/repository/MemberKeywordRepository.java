package umc.cockple.demo.domain.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberKeyword;

import java.util.List;

public interface MemberKeywordRepository extends JpaRepository<MemberKeyword, Long> {

    void deleteAllByMember(Member member);

    List<MemberKeyword> findAllByMemberId(Long memberId);
}
