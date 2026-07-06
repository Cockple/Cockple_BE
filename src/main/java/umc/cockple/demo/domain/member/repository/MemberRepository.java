package umc.cockple.demo.domain.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import umc.cockple.demo.domain.member.domain.Member;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {

    default Map<Long, String> findMemberNamesByIds(Set<Long> memberIds) {
        if (memberIds.isEmpty()) {
            return Map.of();
        }

        return findMemberNameMapsByIds(memberIds).stream()
                .collect(Collectors.toMap(
                        map -> (Long) map.get("id"),
                        map -> (String) map.get("name")
                ));
    }

    @Query("""
            SELECT new map(m.id as id, m.memberName as name) FROM Member m 
            WHERE m.id IN :memberIds
            """)
    List<Map<String, Object>> findMemberNameMapsByIds(@Param("memberIds") Set<Long> memberIds);


    Optional<Member> findBySocialId(Long socialId);

    @Query("""
            SELECT m FROM Member m
            LEFT JOIN FETCH m.addresses addr
            WHERE m.id = :memberId
            AND m.isActive = 'ACTIVE'
            """)
    Optional<Member> findMemberWithAddresses(Long memberId);

    @Query("""
            SELECT m FROM Member m
            LEFT JOIN FETCH m.profileImg 
            WHERE m.id = :memberId
            AND m.isActive = 'ACTIVE'
            """)
    Optional<Member> findMemberWithProfileById(@Param("memberId") Long memberId);

    /** 토큰 버전 원자적 증가 (탈퇴/재사용 탐지 시 발급된 모든 토큰 무효화) */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Member m SET m.tokenVersion = m.tokenVersion + 1 WHERE m.id = :memberId")
    void incrementTokenVersion(@Param("memberId") Long memberId);

    /** 토큰 버전만 조회 (Redis 캐시 miss 시 SoT fallback) */
    @Query("SELECT m.tokenVersion FROM Member m WHERE m.id = :memberId")
    Optional<Long> findTokenVersionById(@Param("memberId") Long memberId);

}
