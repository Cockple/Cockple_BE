package umc.cockple.demo.domain.contest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.cockple.demo.domain.contest.domain.Contest;

public interface ContestRepository extends JpaRepository<Contest, Long> {
}
