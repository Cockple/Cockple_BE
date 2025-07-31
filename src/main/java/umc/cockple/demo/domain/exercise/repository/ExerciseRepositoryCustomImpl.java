package umc.cockple.demo.domain.exercise.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.domain.QExercise;
import umc.cockple.demo.domain.exercise.dto.ExerciseRecommendationCalendarDTO;
import umc.cockple.demo.domain.member.domain.QMember;
import umc.cockple.demo.domain.member.domain.QMemberExercise;
import umc.cockple.demo.domain.member.domain.QMemberParty;
import umc.cockple.demo.domain.party.domain.QParty;

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
@Slf4j
public class ExerciseRepositoryCustomImpl implements ExerciseRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final QExercise exercise = QExercise.exercise;
    private final QParty party = QParty.party;
    private final QMember member = QMember.member;
    private final QMemberParty memberParty = QMemberParty.memberParty;
    private final QMemberExercise memberExercise = QMemberExercise.memberExercise;

    @Override
    public List<Exercise> findFilteredRecommendedExercisesForCalendar(
            Long memberId, Integer memberAge, ExerciseRecommendationCalendarDTO.FilterSortType filterSortType,
            LocalDate startDate, LocalDate endDate) {

        log.info("필터링된 추천 운동 조회 시작 - memberId: {}, 기간: {} ~ {}", memberId, startDate, endDate);

        BooleanBuilder whereClause = new BooleanBuilder();

        addBaseConditions(whereClause, memberId, memberAge, startDate, endDate);

        return List.of();
    }

    private void addBaseConditions(BooleanBuilder whereClause, Long memberId, Integer memberAge,
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

        whereClause.and(party.minAge.loe(memberAge))
                .and(party.maxAge.goe(memberAge));

        whereClause.and(exercise.outsideGuestAccept.eq(true));
    }
}
