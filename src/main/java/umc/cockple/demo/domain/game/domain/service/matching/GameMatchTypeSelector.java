package umc.cockple.demo.domain.game.domain.service.matching;

import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.game.domain.GameBoardMember;
import umc.cockple.demo.domain.game.enums.GameMatchType;
import umc.cockple.demo.domain.game.exception.GameErrorCode;
import umc.cockple.demo.domain.game.exception.GameException;
import umc.cockple.demo.global.enums.Gender;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class GameMatchTypeSelector {

    public List<GameMatchType> findAvailableTypes(List<GameBoardMember> candidates) {
        long maleCount = countByGender(candidates, Gender.MALE);
        long femaleCount = countByGender(candidates, Gender.FEMALE);

        List<GameMatchType> availableTypes = new ArrayList<>();
        if (maleCount >= 2 && femaleCount >= 2) {
            availableTypes.add(GameMatchType.MIXED_DOUBLES);
        }
        if (maleCount >= 4) {
            availableTypes.add(GameMatchType.MEN_DOUBLES);
        }
        if (femaleCount >= 4) {
            availableTypes.add(GameMatchType.WOMEN_DOUBLES);
        }
        if (availableTypes.isEmpty()) {
            throw new GameException(GameErrorCode.INSUFFICIENT_GENDER_COMPOSITION);
        }
        return availableTypes;
    }

    public GameMatchType selectFrom(List<GameMatchType> availableTypes) {
        if (availableTypes.isEmpty()) {
            throw new GameException(GameErrorCode.INSUFFICIENT_GENDER_COMPOSITION);
        }
        int selectedIndex = ThreadLocalRandom.current().nextInt(availableTypes.size());
        return availableTypes.get(selectedIndex);
    }

    private long countByGender(List<GameBoardMember> candidates, Gender gender) {
        return candidates.stream().filter(candidate -> candidate.getGender() == gender).count();
    }
}
