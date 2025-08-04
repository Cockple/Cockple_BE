package umc.cockple.demo.domain.member.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.party.domain.Party;

public interface MemberRepositoryCustom {
    Slice<Member> findRecommendedMembers(Party party, String levelSearch, Pageable pageable);
}
