package umc.cockple.demo.domain.party.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.domain.PartyKeyword;
import umc.cockple.demo.global.enums.Keyword;

public interface PartyKeywordRepository extends JpaRepository<PartyKeyword, Long> {
    boolean existsByPartyAndKeyword(Party party, Keyword keyword);
}
