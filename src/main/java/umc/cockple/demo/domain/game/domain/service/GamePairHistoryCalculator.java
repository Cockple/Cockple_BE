package umc.cockple.demo.domain.game.domain.service;

import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.game.domain.Game;
import umc.cockple.demo.domain.game.domain.GamePlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class GamePairHistoryCalculator {

    public GamePairHistory calculate(List<Game> completedGames) {
        Map<MemberPair, Integer> completedGameCounts = new HashMap<>();
        for (Game completedGame : completedGames) {
            for (MemberPair pair : pairsOf(completedGame)) {
                completedGameCounts.merge(pair, 1, Integer::sum);
            }
        }

        Set<MemberPair> lastGamePairs = completedGames.stream()
                .max(Comparator.comparing(Game::getCompletedAt))
                .map(this::pairsOf)
                .map(Set::copyOf)
                .orElseGet(Set::of);

        return new GamePairHistory(Map.copyOf(completedGameCounts), lastGamePairs);
    }

    public GamePairHistory fromCounts(
            List<GamePairCount> pairCounts,
            List<Long> lastGameMemberIds) {
        Map<MemberPair, Integer> completedGameCounts = new HashMap<>();
        pairCounts.forEach(pairCount -> completedGameCounts.put(
                MemberPair.of(pairCount.memberIdA(), pairCount.memberIdB()),
                Math.toIntExact(pairCount.count())));

        List<MemberPair> lastGamePairs = pairsOf(lastGameMemberIds);
        return new GamePairHistory(
                Map.copyOf(completedGameCounts),
                Set.copyOf(lastGamePairs));
    }

    private List<MemberPair> pairsOf(Game game) {
        List<Long> memberIds = game.getPlayers().stream()
                .map(GamePlayer::getGameBoardMember)
                .map(gameBoardMember -> gameBoardMember.getId())
                .distinct()
                .sorted()
                .toList();

        return pairsOf(memberIds);
    }

    private List<MemberPair> pairsOf(List<Long> memberIds) {
        List<MemberPair> pairs = new ArrayList<>();
        for (int i = 0; i < memberIds.size(); i++) {
            for (int j = i + 1; j < memberIds.size(); j++) {
                pairs.add(MemberPair.of(memberIds.get(i), memberIds.get(j)));
            }
        }
        return pairs;
    }

    public static final class GamePairHistory {

        private final Map<MemberPair, Integer> completedGameCounts;
        private final Set<MemberPair> lastGamePairs;

        private GamePairHistory(
                Map<MemberPair, Integer> completedGameCounts,
                Set<MemberPair> lastGamePairs) {
            this.completedGameCounts = completedGameCounts;
            this.lastGamePairs = lastGamePairs;
        }

        public int count(Long memberIdA, Long memberIdB) {
            return completedGameCounts.getOrDefault(MemberPair.of(memberIdA, memberIdB), 0);
        }

        public boolean playedInLastGame(Long memberIdA, Long memberIdB) {
            return lastGamePairs.contains(MemberPair.of(memberIdA, memberIdB));
        }
    }

    private record MemberPair(Long lowerMemberId, Long higherMemberId) {

        private static MemberPair of(Long memberIdA, Long memberIdB) {
            if (memberIdA.compareTo(memberIdB) <= 0) {
                return new MemberPair(memberIdA, memberIdB);
            }
            return new MemberPair(memberIdB, memberIdA);
        }
    }
}
