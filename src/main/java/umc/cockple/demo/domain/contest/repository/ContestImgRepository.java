package umc.cockple.demo.domain.contest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.cockple.demo.domain.contest.domain.ContestImg;

public interface ContestImgRepository extends JpaRepository<ContestImg,Long> {
}
