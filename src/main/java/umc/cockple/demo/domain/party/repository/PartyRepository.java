package umc.cockple.demo.domain.party.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.cockple.demo.domain.party.domain.Party;

public interface PartyRepository extends JpaRepository<Party, Long> {
}
