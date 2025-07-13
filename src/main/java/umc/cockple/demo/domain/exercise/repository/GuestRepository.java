package umc.cockple.demo.domain.exercise.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.cockple.demo.domain.exercise.domain.Guest;

public interface GuestRepository extends JpaRepository<Guest, Long> {
}
