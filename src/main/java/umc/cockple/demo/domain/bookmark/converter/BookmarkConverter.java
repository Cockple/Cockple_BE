package umc.cockple.demo.domain.bookmark.converter;

import umc.cockple.demo.domain.bookmark.domain.ExerciseBookmark;
import umc.cockple.demo.domain.bookmark.dto.GetAllExerciseBookmarksResponseDTO;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.domain.PartyLevel;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;

import java.util.List;

public class BookmarkConverter {
    public static GetAllExerciseBookmarksResponseDTO exerciseBookmarkToDTO(ExerciseBookmark bookmark,
                                                                           boolean includeParty, boolean includeExercise) {
        Exercise exercise = bookmark.getExercise();

        return GetAllExerciseBookmarksResponseDTO.builder()
                .exerciseId(exercise.getId())
                .partyName(exercise.getParty().getPartyName())
                .buildingName(exercise.getExerciseAddr().getBuildingName())
                .streetAddr(exercise.getExerciseAddr().getStreetAddr())
                .femaleLevel(getLevelList(exercise.getParty(), Gender.FEMALE))
                .maleLevel(getLevelList(exercise.getParty() , Gender.MALE))
                .date(exercise.getDate())
                .startExerciseTime(exercise.getStartTime())
                .endExerciseTime(exercise.getEndTime())
                .maxMemberCnt(exercise.getMaxCapacity())
                .nowMemberCnt(exercise.getNowCapacity())
                .includeParty(includeParty)
                .includeExercise(includeExercise)
                .build();
    }

    private  static List<Level> getLevelList(Party party, Gender gender) {
        return party.getLevels().stream()
                .filter(lever -> lever.getGender() == gender)
                .map(PartyLevel::getLevel)
                .toList();
    }
}
