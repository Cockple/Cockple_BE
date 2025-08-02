package umc.cockple.demo.domain.exercise.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.domain.QExercise;
import umc.cockple.demo.domain.exercise.domain.QExerciseAddr;
import umc.cockple.demo.domain.exercise.dto.ExerciseRecommendationCalendarDTO;
import umc.cockple.demo.domain.member.domain.QMemberExercise;
import umc.cockple.demo.domain.member.domain.QMemberParty;
import umc.cockple.demo.domain.party.domain.QParty;
import umc.cockple.demo.domain.party.domain.QPartyImg;
import umc.cockple.demo.domain.party.domain.QPartyLevel;

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
@Slf4j
public class ExerciseRepositoryCustomImpl implements ExerciseRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private final QExercise exercise = QExercise.exercise;
    private final QExerciseAddr addr = QExerciseAddr.exerciseAddr;
    private final QParty party = QParty.party;
    private final QPartyLevel partyLevel = QPartyLevel.partyLevel;
    private final QMemberParty memberParty = QMemberParty.memberParty;
    private final QMemberExercise memberExercise = QMemberExercise.memberExercise;
    private final QPartyImg partyImg = QPartyImg.partyImg;

    @Override
    public List<Exercise> findFilteredRecommendedExercisesForCalendar(
            Long memberId, Integer memberBirthYear, ExerciseRecommendationCalendarDTO.FilterSortType filterSortType,
            LocalDate startDate, LocalDate endDate) {

        log.info("필터링된 추천 운동 조회 시작 - memberId: {}, 기간: {} ~ {}", memberId, startDate, endDate);

        BooleanBuilder whereClause = new BooleanBuilder();

        addBaseConditions(whereClause, memberId, memberBirthYear, startDate, endDate);
        addDynamicFilters(whereClause, filterSortType);

        List<Exercise> exercises = queryFactory
                .selectFrom(exercise)
                .join(exercise.exerciseAddr, addr).fetchJoin()
                .join(exercise.party, party).fetchJoin()
                .leftJoin(party.partyImg, partyImg).fetchJoin()
                .where(whereClause)
                .fetch();

        log.info("필터링된 추천 운동 조회 완료 - 조회된 운동 수: {}", exercises.size());

        return exercises;
    }

    private void addBaseConditions(BooleanBuilder whereClause, Long memberId, Integer memberBirthYear,
                                   LocalDate startDate, LocalDate endDate) {
        whereClause.and(exercise.date.between(startDate, endDate));

        whereClause.and(
                JPAExpressions.selectOne()
                        .from(memberParty)
                        .where(memberParty.party.id.eq(party.id)
                                .and(memberParty.member.id.eq(memberId)))
                        .notExists()
        );

        whereClause.and(
                JPAExpressions.selectOne()
                        .from(memberExercise)
                        .where(memberExercise.exercise.id.eq(exercise.id)
                                .and(memberExercise.member.id.eq(memberId)))
                        .notExists()
        );

        whereClause.and(party.minBirthYear.loe(memberBirthYear))
                .and(party.maxBirthYear.goe(memberBirthYear));

        whereClause.and(exercise.outsideGuestAccept.eq(true));
    }

    private void addDynamicFilters(BooleanBuilder whereClause, ExerciseRecommendationCalendarDTO.FilterSortType filterSortType) {
        if (filterSortType.addr1() != null) {
            whereClause.and(exercise.exerciseAddr.addr1.eq(filterSortType.addr1()));

            if (filterSortType.addr2() != null)
                whereClause.and(exercise.exerciseAddr.addr2.eq(filterSortType.addr2()));
        }

        if (filterSortType.levels() != null && !filterSortType.levels().isEmpty()) {
            whereClause.and(
                    JPAExpressions.selectOne()
                            .from(partyLevel)
                            .where(partyLevel.party.id.eq(party.id)
                                    .and(partyLevel.level.in(filterSortType.levels())))
                            .exists()
            );
        }

        if (filterSortType.participationTypes() != null) {
            whereClause.and(party.partyType.in(filterSortType.participationTypes()));
        }

        if (filterSortType.activityTimes() != null) {
            whereClause.and(party.activityTime.in(filterSortType.activityTimes()));
        }
    }
}
