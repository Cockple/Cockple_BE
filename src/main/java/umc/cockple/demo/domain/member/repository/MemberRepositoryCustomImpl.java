package umc.cockple.demo.domain.member.repository;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.util.StringUtils;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.domain.PartyLevel;
import umc.cockple.demo.domain.party.enums.RequestStatus;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;

import java.util.List;
import java.util.stream.Stream;

import static umc.cockple.demo.domain.member.domain.QMember.member;
import static umc.cockple.demo.domain.member.domain.QMemberAddr.memberAddr;
import static umc.cockple.demo.domain.member.domain.QMemberParty.memberParty;
import static umc.cockple.demo.domain.party.domain.QPartyJoinRequest.partyJoinRequest;

@RequiredArgsConstructor
public class MemberRepositoryCustomImpl implements MemberRepositoryCustom{

    public final JPAQueryFactory queryFactory;

    @Override
    public Slice<Member> findRecommendedMembers(Party party, String levelSearch, Pageable pageable) {
        //추천에서 제외할 멤버를 찾는 로직
        List<Long> excludedMemberIds = findExcludedMemberIds(party);

        //동적 쿼리로 데이터 조회
        List<Member> content = queryFactory
                .selectFrom(member)
                .join(member.addresses, memberAddr).fetchJoin()
                .where(
                        //제외할 멤버 목록에 포함되지 않는 사용자
                        member.id.notIn(excludedMemberIds),
                        //급수, 지역, 나이로 필터링
                        levelMatch(party, levelSearch),
                        member.birth.year().between(party.getMinBirthYear(), party.getMaxBirthYear()),
                        memberAddr.isMain.isTrue().and(memberAddr.addr1.eq(party.getPartyAddr().getAddr1()))
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1)
                .fetch();

        boolean hasNext = false;
        if (content.size() > pageable.getPageSize()) {
            content.remove(pageable.getPageSize());
            hasNext = true;
        }

        return new SliceImpl<>(content, pageable, hasNext);
    }

    private List<Long> findExcludedMemberIds(Party party) {
        //이미 가입한 멤버 id 리스트
        List<Long> existingMemberIds = queryFactory
                .select(memberParty.member.id)
                .from(memberParty)
                .where(memberParty.party.eq(party))
                .fetch();

        //가입 신청 대기 중인 사용자 id 리스트
        List<Long> pendingMemberIds = queryFactory
                .select(partyJoinRequest.member.id)
                .from(partyJoinRequest)
                .where(
                        partyJoinRequest.party.eq(party),
                        partyJoinRequest.status.eq(RequestStatus.PENDING)
                )
                .fetch();

        return Stream.concat(existingMemberIds.stream(), pendingMemberIds.stream()).distinct().toList();
    }

    private BooleanExpression levelMatch(Party party, String levelSearch) {
        if (StringUtils.hasText(levelSearch)) {
            //쿼리 파라미터로 받은 검색 급수만 필터링
            return member.level.eq(Level.fromKorean(levelSearch));
        } else {
            //모임의 급수 조건에 맞는 사용자 필터링
            List<Level> femaleLevels = party.getLevels().stream()
                    .filter(pl -> pl.getGender() == Gender.FEMALE)
                    .map(PartyLevel::getLevel).toList();
            List<Level> maleLevels = party.getLevels().stream()
                    .filter(pl -> pl.getGender() == Gender.MALE)
                    .map(PartyLevel::getLevel).toList();
            return member.gender.eq(Gender.FEMALE).and(member.level.in(femaleLevels))
                    .or(member.gender.eq(Gender.MALE).and(member.level.in(maleLevels)));
        }
    }
}
