package umc.cockple.demo.domain.bookmark.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.cockple.demo.domain.bookmark.domain.PartyBookmark;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.party.domain.Party;

import java.util.Map;
import java.util.Optional;

public interface PartyBookmarkRepository extends JpaRepository<PartyBookmark, Long> {

    boolean existsByMemberAndParty(Member member, Party party);

    Optional<PartyBookmark> findByMemberAndParty(Member member, Party party);
}
