package umc.cockple.demo.domain.member.service.query.lookup;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberAddr;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.exception.MemberException;
import umc.cockple.demo.domain.member.repository.MemberRepository;

import java.util.Map;
import java.util.Set;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberLookupService {

    private final MemberRepository memberRepository;

    public Member findByIdOrThrow(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
    }

    public Member findWithAddressesOrThrow(Long memberId) {
        return memberRepository.findMemberWithAddresses(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
    }

    public MemberAddr findMainAddressOrThrow(Member member) {
        return member.getAddresses().stream()
                .filter(MemberAddr::getIsMain)
                .findFirst()
                .orElseThrow(() -> new MemberException(MemberErrorCode.MAIN_ADDRESS_NULL));
    }

    public Map<Long, String> findNamesByIds(Set<Long> memberIds) {
        return memberRepository.findMemberNamesByIds(memberIds);
    }
}
