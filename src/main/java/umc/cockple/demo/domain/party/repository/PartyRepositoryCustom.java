package umc.cockple.demo.domain.party.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.dto.PartyFilterDTO;

public interface PartyRepositoryCustom {
    //필터 모드를 위한 동적 쿼리 메서드
    Slice<Party> searchParties(Long memberId, PartyFilterDTO filter, Pageable pageable);
}