package umc.cockple.demo.domain.contest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import umc.cockple.demo.domain.contest.domain.Contest;

import java.util.List;
import java.util.Optional;

public interface ContestRepository extends JpaRepository<Contest, Long> {
    Optional<Contest> findByIdAndMember_Id(Long contestId, Long memberId);

    List<Contest> findAllByMember_Id(Long memberId);

    @Query("SELECT COUNT(c) FROM Contest c WHERE c.member.id = :memberId")
    int countAllMedalsByMemberId(@Param("memberId") Long memberId);

    @Query("SELECT COUNT(c) FROM Contest c WHERE c.member.id = :memberId AND c.medalType = 'GOLD'")
    int countGoldMedalsByMemberId(@Param("memberId") Long memberId);

    @Query("SELECT COUNT(c) FROM Contest c WHERE c.member.id = :memberId AND c.medalType = 'SILVER'")
    int countSilverMedalsByMemberId(@Param("memberId") Long memberId);

    @Query("SELECT COUNT(c) FROM Contest c WHERE c.member.id = :memberId AND c.medalType = 'BRONZE'")
    int countBronzeMedalsByMemberId(@Param("memberId") Long memberId);
}
