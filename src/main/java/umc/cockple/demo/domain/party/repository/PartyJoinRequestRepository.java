package umc.cockple.demo.domain.party.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.cockple.demo.domain.party.domain.PartyJoinRequest;

public interface PartyJoinRequestRepository extends JpaRepository<PartyJoinRequest, Long> {
}
