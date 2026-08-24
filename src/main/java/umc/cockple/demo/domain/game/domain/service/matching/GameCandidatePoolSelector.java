package umc.cockple.demo.domain.game.domain.service.matching;

import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.game.domain.GameBoardMember;
import umc.cockple.demo.domain.game.enums.GameMatchType;
import umc.cockple.demo.domain.game.exception.GameErrorCode;
import umc.cockple.demo.domain.game.exception.GameException;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class GameCandidatePoolSelector {

    private static final int MAX_GAME_COUNT_GAP = 5;
    private static final int MAX_CANDIDATE_POOL_SIZE = 12;
    private static final int REQUIRED_PLAYER_COUNT = 4;

    private static final Comparator<GameBoardMember> FAIRNESS_ORDER = Comparator
            .comparingInt(GameBoardMember::getGameCount)
            .thenComparing(GameBoardMember::getId);

    public List<GameBoardMember> select(
            List<GameBoardMember> availableMembers,
            GameMatchType matchType) {
        List<GameBoardMember> sortedCandidates = availableMembers.stream()
                .filter(member -> member.getLevel() != Level.NONE)
                .filter(member -> belongsToMatchType(member, matchType))
                .sorted(FAIRNESS_ORDER)
                .toList();
        if (sortedCandidates.isEmpty()) {
            throw randomMatchNotFound();
        }

        int minimumGameCount = sortedCandidates.get(0).getGameCount();
        for (int gap = 0; gap <= MAX_GAME_COUNT_GAP; gap++) {
            int maximumGameCount = minimumGameCount + gap;
            List<GameBoardMember> expandedPool = sortedCandidates.stream()
                    .filter(member -> member.getGameCount() <= maximumGameCount)
                    .toList();
            if (hasRequiredComposition(expandedPool, matchType)) {
                return limitSize(expandedPool, matchType);
            }
        }
        throw randomMatchNotFound();
    }

    private boolean belongsToMatchType(GameBoardMember member, GameMatchType matchType) {
        return switch (matchType) {
            case MIXED_DOUBLES -> true;
            case MEN_DOUBLES -> member.getGender() == Gender.MALE;
            case WOMEN_DOUBLES -> member.getGender() == Gender.FEMALE;
        };
    }

    private boolean hasRequiredComposition(
            List<GameBoardMember> candidates,
            GameMatchType matchType) {
        if (matchType == GameMatchType.MIXED_DOUBLES) {
            return countByGender(candidates, Gender.MALE) >= 2
                    && countByGender(candidates, Gender.FEMALE) >= 2;
        }
        return candidates.size() >= REQUIRED_PLAYER_COUNT;
    }

    private List<GameBoardMember> limitSize(
            List<GameBoardMember> candidates,
            GameMatchType matchType) {
        if (candidates.size() <= MAX_CANDIDATE_POOL_SIZE) {
            return candidates;
        }
        if (matchType != GameMatchType.MIXED_DOUBLES) {
            return candidates.subList(0, MAX_CANDIDATE_POOL_SIZE);
        }

        List<GameBoardMember> selected = new ArrayList<>();
        addLowestGameCountMembers(selected, candidates, Gender.MALE, 2);
        addLowestGameCountMembers(selected, candidates, Gender.FEMALE, 2);

        Set<Long> selectedIds = new HashSet<>();
        selected.forEach(member -> selectedIds.add(member.getId()));
        for (GameBoardMember candidate : candidates) {
            if (selected.size() == MAX_CANDIDATE_POOL_SIZE) {
                break;
            }
            if (selectedIds.add(candidate.getId())) {
                selected.add(candidate);
            }
        }
        return selected.stream().sorted(FAIRNESS_ORDER).toList();
    }

    private void addLowestGameCountMembers(
            List<GameBoardMember> selected,
            List<GameBoardMember> candidates,
            Gender gender,
            int count) {
        candidates.stream()
                .filter(candidate -> candidate.getGender() == gender)
                .limit(count)
                .forEach(selected::add);
    }

    private long countByGender(List<GameBoardMember> members, Gender gender) {
        return members.stream()
                .filter(member -> member.getGender() == gender)
                .count();
    }

    private GameException randomMatchNotFound() {
        return new GameException(GameErrorCode.RANDOM_MATCH_NOT_FOUND);
    }
}
