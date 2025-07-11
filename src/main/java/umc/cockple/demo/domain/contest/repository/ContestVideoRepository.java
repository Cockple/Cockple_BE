package umc.cockple.demo.domain.contest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.cockple.demo.domain.contest.domain.ContestVideo;

public interface ContestVideoRepository extends JpaRepository<ContestVideo,Long> {
}
