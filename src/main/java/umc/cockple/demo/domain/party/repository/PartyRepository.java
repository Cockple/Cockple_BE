package umc.cockple.demo.domain.party.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;

import java.util.List;
import java.util.Optional;

public interface PartyRepository extends JpaRepository<Party, Long> {

    @Query("""
            SELECT p FROM Party p JOIN p.memberParties mp
            WHERE mp.member.id = :memberId
            AND (:created = false OR p.ownerId = :memberId)
            """) //created가 true라면 p.ownerId = :memberId로 내가 만든 모임을 조회 (false라면 모든 내 모임 조회)
    Slice<Party> findMyParty(@Param("memberId") Long memberId, @Param("created") boolean created, Pageable pageable);

    @Query("""
            SELECT p FROM Party p 
            LEFT JOIN FETCH p.levels pl
            WHERE p.id = :partyId
            """)
    Optional<Party> findByIdWithLevels(@Param("partyId") Long partyId);

    @Query("""
        SELECT p FROM Party p
        WHERE p.partyAddr.addr1 = :addr1
        AND p.minAge <= :birthYear AND p.maxAge >= :birthYear
        AND EXISTS (
            SELECT pl FROM p.levels pl
            WHERE pl.gender = :gender AND pl.level = :level
        )
        AND NOT EXISTS (
            SELECT mp FROM MemberParty mp
            WHERE mp.member.id = :memberId AND mp.party.id = p.id
        ) 
        ORDER BY p.createdAt DESC
    """)
    List<Party> findRecommendedParties(
            @Param("addr1") String addr1,
            @Param("birthYear") int birthYear,
            @Param("gender") Gender gender,
            @Param("level") Level level,
            @Param("memberId") Long memberId
    );
}
