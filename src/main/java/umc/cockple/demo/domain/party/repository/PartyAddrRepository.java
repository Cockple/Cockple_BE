package umc.cockple.demo.domain.party.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.cockple.demo.domain.party.domain.PartyAddr;

import java.util.Optional;

public interface PartyAddrRepository extends JpaRepository<PartyAddr, Long> {

    // addr1과 addr2를 기준으로 이미 존재하는 주소인지 확인하기 위한 메서드
    Optional<PartyAddr> findByAddr1AndAddr2(String addr1, String addr2);
}
