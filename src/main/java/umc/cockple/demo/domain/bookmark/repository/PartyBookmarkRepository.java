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
import java.util.Set;

public interface PartyBookmarkRepository extends JpaRepository<PartyBookmark, Long> {

    boolean existsByMemberAndParty(Member member, Party party);

    Optional<PartyBookmark> findByMemberAndParty(Member member, Party party);

    /**
     * 찜한 모임 목록 조회 시 사용. 연관 엔티티를 한 번에 가져와 N+1을 제거한다.
     * - toOne(partyAddr, partyImg)은 fetch join으로 즉시 로딩
     * - party의 역방향 @OneToOne(chatRoom)은 LAZY여도 Hibernate가 party마다 즉시 조회하므로
     *   (eager라 batch로도 안 묶임) 함께 fetch join해 N+1을 제거한다.
     * - 컬렉션은 한 쿼리에 여러 bag을 fetch join할 수 없어(MultipleBagFetchException)
     *   exercises만 fetch join하고, levels는 배치로 로딩된다.
     */
    @Query("""
        SELECT DISTINCT pb
        FROM PartyBookmark pb
        JOIN FETCH pb.party p
        LEFT JOIN FETCH p.partyAddr
        LEFT JOIN FETCH p.partyImg
        LEFT JOIN FETCH p.chatRoom
        LEFT JOIN FETCH p.exercises
        WHERE pb.member = :member
        """)
    List<PartyBookmark> findAllByMemberWithParty(@Param("member") Member member);

    @Query("""
        SELECT pb.party.id
        FROM PartyBookmark pb
        WHERE pb.member.id = :memberId
        """)
    Set<Long> findAllPartyIdsByMemberId(@Param("memberId") Long memberId);

    List<PartyBookmark> findAllByMember(Member member);

    Optional<PartyBookmark> findFirstByMemberOrderByCreatedAtAsc(Member member);
}
