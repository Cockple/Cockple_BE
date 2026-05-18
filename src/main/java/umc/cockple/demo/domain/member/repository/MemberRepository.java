package umc.cockple.demo.domain.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.enums.MemberStatus;

import java.time.LocalDateTime;
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
            LEFT JOIN FETCH m.profileImg
            WHERE m.isActive = :isActive
            AND m.deletedAt < :threshold
            """)
    List<Member> findAllByIsActiveAndDeletedAtBefore(@Param("isActive") MemberStatus isActive, @Param("threshold") LocalDateTime threshold);

    @Modifying
    @Query("DELETE FROM Member m WHERE m.id IN :memberIds")
    void deleteByMemberIds(@Param("memberIds") List<Long> memberIds);

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

}
