package umc.cockple.demo.domain.contest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.cockple.demo.domain.contest.domain.Contest;

import java.util.List;
import java.util.Optional;

public interface ContestRepository extends JpaRepository<Contest, Long> {
    Optional<Contest> findByIdAndMember_Id(Long contestId, Long memberId);

    List<Contest> findAllByMember_Id(Long memberId);
}
