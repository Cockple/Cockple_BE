package umc.cockple.demo.domain.exercise.repository.support;

import umc.cockple.demo.domain.party.enums.ActivityTime;
import umc.cockple.demo.domain.party.enums.ParticipationType;
import umc.cockple.demo.global.enums.Level;

import java.util.List;

/*
 * 추천 운동 필터 조회를 위한 repository-facing 검색 조건 값 객체다.
 *
 * Controller/API DTO는 HTTP 요청/응답 계약을 표현하고,
 * Repository는 이 내부 조건 객체만 참조해 DTO 패키지 변경이 persistence 경계로 전파되지 않게 한다.
 * sortType은 repository 필터 조건이 아니라 응답 정렬 정책이므로 포함하지 않는다.
 */
public record ExerciseRecommendationSearchCondition(
        String addr1,
        String addr2,
        List<Level> levels,
        List<ParticipationType> participationTypes,
        List<ActivityTime> activityTimes
) {
}
