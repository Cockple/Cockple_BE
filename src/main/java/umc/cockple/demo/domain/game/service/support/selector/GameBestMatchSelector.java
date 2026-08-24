package umc.cockple.demo.domain.game.service.support.selector;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.game.domain.GameBoardMember;
import umc.cockple.demo.domain.game.enums.GameMatchType;
import umc.cockple.demo.domain.game.exception.GameErrorCode;
import umc.cockple.demo.domain.game.exception.GameException;
import umc.cockple.demo.domain.game.service.support.calculator.GameAbilityScoreCalculator;
import umc.cockple.demo.domain.game.service.support.calculator.GamePairHistoryCalculator.GamePairHistory;
import umc.cockple.demo.global.enums.Gender;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GameBestMatchSelector {

    private static final int REQUIRED_PLAYER_COUNT = 4;
    private static final int BALANCE_WEIGHT = 10;
    private static final int DUPLICATE_WEIGHT = 15;

    private final GameAbilityScoreCalculator abilityScoreCalculator;

    public List<Long> select(
            List<GameBoardMember> candidatePool,
            GameMatchType matchType,
            GamePairHistory pairHistory) {
        MatchScore bestMatch = null;
        int size = candidatePool.size();
        for (int first = 0; first < size - 3; first++) {
            for (int second = first + 1; second < size - 2; second++) {
                for (int third = second + 1; third < size - 1; third++) {
                    for (int fourth = third + 1; fourth < size; fourth++) {
                        List<GameBoardMember> members = List.of(
                                candidatePool.get(first),
                                candidatePool.get(second),
                                candidatePool.get(third),
                                candidatePool.get(fourth));
                        if (!matchesType(members, matchType)) {
                            continue;
                        }

                        MatchScore score = score(members, matchType, pairHistory);
                        if (bestMatch == null || score.compareTo(bestMatch) < 0) {
                            bestMatch = score;
                        }
                    }
                }
            }
        }
        if (bestMatch == null) {
            throw new GameException(GameErrorCode.RANDOM_MATCH_NOT_FOUND);
        }
        return bestMatch.memberIds();
    }

    private boolean matchesType(List<GameBoardMember> members, GameMatchType matchType) {
        if (members.size() != REQUIRED_PLAYER_COUNT) {
            return false;
        }
        long maleCount = countByGender(members, Gender.MALE);
        long femaleCount = countByGender(members, Gender.FEMALE);
        return switch (matchType) {
            case MIXED_DOUBLES -> maleCount == 2 && femaleCount == 2;
            case MEN_DOUBLES -> maleCount == 4;
            case WOMEN_DOUBLES -> femaleCount == 4;
        };
    }

    private MatchScore score(
            List<GameBoardMember> members,
            GameMatchType matchType,
            GamePairHistory pairHistory) {
        int balanceScore = minimumTeamDifference(members, matchType) * BALANCE_WEIGHT;
        int duplicateScore = duplicateScore(members, pairHistory);
        int fairnessScore = members.stream()
                .mapToInt(GameBoardMember::getGameCount)
                .sum();
        int totalScore = balanceScore + duplicateScore * DUPLICATE_WEIGHT + fairnessScore;
        List<Long> memberIds = members.stream()
                .map(GameBoardMember::getId)
                .sorted()
                .toList();
        return new MatchScore(totalScore, duplicateScore, fairnessScore, memberIds);
    }

    private int minimumTeamDifference(
            List<GameBoardMember> members,
            GameMatchType matchType) {
        if (matchType == GameMatchType.MIXED_DOUBLES) {
            List<GameBoardMember> males = byGender(members, Gender.MALE);
            List<GameBoardMember> females = byGender(members, Gender.FEMALE);
            return Math.min(
                    teamDifference(males.get(0), females.get(0), males.get(1), females.get(1)),
                    teamDifference(males.get(0), females.get(1), males.get(1), females.get(0)));
        }
        return Math.min(
                teamDifference(members.get(0), members.get(1), members.get(2), members.get(3)),
                Math.min(
                        teamDifference(members.get(0), members.get(2), members.get(1), members.get(3)),
                        teamDifference(members.get(0), members.get(3), members.get(1), members.get(2))));
    }

    private int teamDifference(
            GameBoardMember firstA,
            GameBoardMember firstB,
            GameBoardMember secondA,
            GameBoardMember secondB) {
        int firstTeamScore = abilityScoreCalculator.calculate(firstA)
                + abilityScoreCalculator.calculate(firstB);
        int secondTeamScore = abilityScoreCalculator.calculate(secondA)
                + abilityScoreCalculator.calculate(secondB);
        return Math.abs(firstTeamScore - secondTeamScore);
    }

    private int duplicateScore(
            List<GameBoardMember> members,
            GamePairHistory pairHistory) {
        int score = 0;
        for (int first = 0; first < members.size(); first++) {
            for (int second = first + 1; second < members.size(); second++) {
                score += pairHistory.count(
                        members.get(first).getId(),
                        members.get(second).getId());
            }
        }
        return score;
    }

    private List<GameBoardMember> byGender(
            List<GameBoardMember> members,
            Gender gender) {
        return members.stream()
                .filter(member -> member.getGender() == gender)
                .toList();
    }

    private long countByGender(List<GameBoardMember> members, Gender gender) {
        return members.stream()
                .filter(member -> member.getGender() == gender)
                .count();
    }

    private record MatchScore(
            int totalScore,
            int duplicateScore,
            int fairnessScore,
            List<Long> memberIds) implements Comparable<MatchScore> {

        @Override
        public int compareTo(MatchScore other) {
            int compared = Integer.compare(totalScore, other.totalScore);
            if (compared != 0) {
                return compared;
            }
            compared = Integer.compare(duplicateScore, other.duplicateScore);
            if (compared != 0) {
                return compared;
            }
            compared = Integer.compare(fairnessScore, other.fairnessScore);
            if (compared != 0) {
                return compared;
            }
            for (int index = 0; index < memberIds.size(); index++) {
                compared = memberIds.get(index).compareTo(other.memberIds.get(index));
                if (compared != 0) {
                    return compared;
                }
            }
            return 0;
        }
    }
}
