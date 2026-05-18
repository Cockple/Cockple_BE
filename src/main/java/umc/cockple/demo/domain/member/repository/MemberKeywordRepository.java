package umc.cockple.demo.domain.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberKeyword;

import java.util.List;

public interface MemberKeywordRepository extends JpaRepository<MemberKeyword, Long> {

    void deleteAllByMember(Member member);

    List<MemberKeyword> findAllByMemberId(Long memberId);

    @Modifying
    @Query("DELETE FROM MemberKeyword mk WHERE mk.member.id IN :memberIds")
    void deleteByMemberIds(@Param("memberIds") List<Long> memberIds);
}
