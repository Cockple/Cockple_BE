package umc.cockple.demo.domain.bookmark.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import umc.cockple.demo.domain.bookmark.domain.PartyBookmark;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.party.domain.Party;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface PartyBookmarkRepository extends JpaRepository<PartyBookmark, Long> {

    boolean existsByMemberAndParty(Member member, Party party);

    Optional<PartyBookmark> findByMemberAndParty(Member member, Party party);

    @Query("""
        SELECT DISTINCT pb
        FROM PartyBookmark pb
        JOIN FETCH pb.party p
        LEFT JOIN FETCH p.exercises
        WHERE pb.member = :member
        """)
    List<PartyBookmark> findAllByMemberWithParty(@Param("member") Member member);
}
