package umc.cockple.demo.domain.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import umc.cockple.demo.domain.member.domain.Member;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public interface MemberRepository extends JpaRepository<Member, Long> {

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
}
