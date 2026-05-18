package umc.cockple.demo.domain.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberAddr;

import java.util.List;
import java.util.Optional;

public interface MemberAddrRepository extends JpaRepository<MemberAddr, Long> {
    Optional<MemberAddr> findByMemberAndIsMain(Member member, Boolean isMain);

    @Modifying
    @Query("DELETE FROM MemberAddr ma WHERE ma.member.id IN :memberIds")
    void deleteByMemberIds(@Param("memberIds") List<Long> memberIds);
}
