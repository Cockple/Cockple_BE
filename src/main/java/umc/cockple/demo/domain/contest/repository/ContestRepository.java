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

    /**
     * 회원의 메달을 종류별로 한 번의 조건부 집계 쿼리로 센다. (기존 GOLD/SILVER/BRONZE 3회 조회 → 1회)
     * CASE에 ELSE가 없어 미매칭 행은 null이 되고 COUNT가 이를 무시하므로 종류별 개수만 집계된다.
     * medal_type은 EnumType.STRING으로 저장되어 문자열 비교한다.
     */
    @Query(value = """
            SELECT
                COUNT(CASE WHEN c.medal_type = 'GOLD'   THEN 1 END) AS gold,
                COUNT(CASE WHEN c.medal_type = 'SILVER' THEN 1 END) AS silver,
                COUNT(CASE WHEN c.medal_type = 'BRONZE' THEN 1 END) AS bronze
            FROM contest c
            WHERE c.member_id = :memberId
            """, nativeQuery = true)
    MedalCountProjection countMedalsByMemberId(@Param("memberId") Long memberId);

}
