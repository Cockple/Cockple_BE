package umc.cockple.demo.domain.party.repository;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.dto.PartyFilterDTO;
import umc.cockple.demo.domain.party.enums.ActiveDay;
import umc.cockple.demo.domain.party.enums.ActivityTime;
import umc.cockple.demo.domain.party.enums.ParticipationType;
import umc.cockple.demo.domain.party.enums.PartyStatus;
import umc.cockple.demo.domain.party.utils.ActivityTimeUtils;
import umc.cockple.demo.global.enums.Keyword;
import umc.cockple.demo.global.enums.Level;

import java.util.ArrayList;
import java.util.List;

import static umc.cockple.demo.domain.member.domain.QMemberParty.memberParty;
import static umc.cockple.demo.domain.party.domain.QParty.party;
import static umc.cockple.demo.domain.party.domain.QPartyActiveDay.partyActiveDay;
import static umc.cockple.demo.domain.party.domain.QPartyKeyword.partyKeyword;
import static umc.cockple.demo.domain.party.domain.QPartyLevel.partyLevel;

@RequiredArgsConstructor
public class PartyRepositoryCustomImpl implements PartyRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Slice<Party> searchParties(Long memberId, PartyFilterDTO.Request filter, Pageable pageable) {
        //동적 쿼리로 데이터 조회
        List<Party> content = queryFactory
                .selectFrom(party)
                .where(
                        //삭제된 모임은 제외
                        party.status.eq(PartyStatus.ACTIVE),
                        //이미 가입한 모임은 제외
                        party.id.notIn(
                                JPAExpressions.select(memberParty.party.id)
                                        .from(memberParty)
                                        .where(memberParty.member.id.eq(memberId))
                        ),
                        //동적 필터 조건
                        addr1Eq(filter.addr1()),
                        addr2Eq(filter.addr2()),
                        levelIn(filter.level()),
                        partyTypeIn(filter.partyType()),
                        activityDayIn(filter.activityDay()),
                        activityTimeIn(filter.activityTime()),
                        keywordIn(filter.keyword())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1) //hasNext 확인을 위해 1개 더 조회
                .orderBy(getOrderSpecifiers(pageable.getSort()))
                .fetch();

        boolean hasNext = false;
        if (content.size() > pageable.getPageSize()) {
            content.remove(pageable.getPageSize()); //1개 더 조회했기에 마지막 데이터 제거
            hasNext = true;
        }

        return new SliceImpl<>(content, pageable, hasNext);
    }

    private BooleanExpression addr1Eq(String addr1) {
        return StringUtils.hasText(addr1) ? party.partyAddr.addr1.eq(addr1) : null;
    }

    private BooleanExpression addr2Eq(String addr2) {
        return StringUtils.hasText(addr2) ? party.partyAddr.addr2.eq(addr2) : null;
    }

    private BooleanExpression partyTypeIn(List<String> partyTypes) {
        if (partyTypes == null || partyTypes.isEmpty()) return null;
        List<ParticipationType> enums = partyTypes.stream().map(ParticipationType::fromKorean).toList();
        return party.partyType.in(enums);
    }

    private BooleanExpression activityTimeIn(List<String> activityTimes) {
        if (activityTimes == null || activityTimes.isEmpty()) return null;
        List<ActivityTime> enums = activityTimes.stream().map(ActivityTime::fromKorean).toList();

        //오후 또는 오전으로 조회 시, 상시도 조회에 포함
        List<ActivityTime> searchConditions = new ArrayList<>(enums); //toList는 수정 불가능한 리스트를 만들기에 새로 생성
        if (ActivityTimeUtils.shouldAddAlways(searchConditions)){
            searchConditions.add(ActivityTime.ALWAYS);
        }

        return party.activityTime.in(searchConditions);
    }

    private BooleanExpression levelIn(List<String> levels) {
        if (levels == null || levels.isEmpty()) return null;
        List<Level> enums = levels.stream().map(Level::fromKorean).toList();
        return party.id.in(
                //다대일이기에 서브쿼리 사용
                JPAExpressions.select(partyLevel.party.id)
                        .from(partyLevel)
                        .where(partyLevel.level.in(enums))
        );
    }

    private BooleanExpression activityDayIn(List<String> activityDays) {
        if (activityDays == null || activityDays.isEmpty()) return null;
        List<ActiveDay> enums = activityDays.stream().map(ActiveDay::fromKorean).toList();
        return party.id.in(
                JPAExpressions.select(partyActiveDay.party.id)
                        .from(partyActiveDay)
                        .where(partyActiveDay.activeDay.in(enums))
        );
    }

    private BooleanExpression keywordIn(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) return null;
        List<Keyword> enums = keywords.stream().map(Keyword::fromKorean).toList();
        return party.id.in(
                JPAExpressions.select(partyKeyword.party.id)
                        .from(partyKeyword)
                        .where(partyKeyword.keyword.in(enums))
        );
    }

    //Pageable의 Sort 객체를 QueryDSL의 OrderSpecifier 배열로 변환하는 헬퍼 메서드
    private OrderSpecifier<?>[] getOrderSpecifiers(Sort sort) {
        List<OrderSpecifier<?>> orders = new ArrayList<>();
        if (sort.isEmpty()) {
            orders.add(new OrderSpecifier<>(Order.DESC, party.createdAt));
        } else {
            sort.forEach(order -> {
                Order direction = order.isAscending() ? Order.ASC : Order.DESC;
                String property = order.getProperty();

                // PathBuilder를 사용하여 문자열로 된 속성 이름을 Q-Type 경로로 변환
                PathBuilder<Party> pathBuilder = new PathBuilder<>(Party.class, "party");
                orders.add(new OrderSpecifier(direction, pathBuilder.get(property)));
            });
        }
        return orders.toArray(new OrderSpecifier[0]);
    }

}