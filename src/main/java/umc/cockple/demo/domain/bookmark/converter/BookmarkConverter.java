package umc.cockple.demo.domain.bookmark.converter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.bookmark.domain.ExerciseBookmark;
import umc.cockple.demo.domain.bookmark.domain.PartyBookmark;
import umc.cockple.demo.domain.bookmark.dto.GetAllExerciseBookmarksResponseDTO;
import umc.cockple.demo.domain.bookmark.dto.GetAllPartyBookmarkResponseDTO;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.image.service.ImageService;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.domain.PartyLevel;
import umc.cockple.demo.domain.party.enums.ActivityTime;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BookmarkConverter {

    private final ImageService imageService;

    public GetAllExerciseBookmarksResponseDTO exerciseBookmarkToDTO(ExerciseBookmark bookmark,
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

    public GetAllPartyBookmarkResponseDTO partyBookmarkToDTO(PartyBookmark partyBookmark, Exercise exercise, ActivityTime activityTime) {
        Party party = partyBookmark.getParty();
        String profileImg = party.getPartyImg() == null ? null : imageService.getUrlFromKey(party.getPartyImg().getImgKey());

        return GetAllPartyBookmarkResponseDTO.builder()
                .partyId(party.getId())
                .partyName(party.getPartyName())
                .addr1(party.getPartyAddr().getAddr1())
                .addr2(party.getPartyAddr().getAddr2())
                .maleLevel(getLevelList(party, Gender.MALE))
                .femaleLevel(getLevelList(party, Gender.FEMALE))
                .latestExerciseDate(exercise == null ? null : exercise.getDate())
                .latestExerciseTime(activityTime)
                .exerciseCnt(party.getExerciseCount())
                .profileImgUrl(profileImg)
                .build();
    }

    private List<Level> getLevelList(Party party, Gender gender) {
        return party.getLevels().stream()
                .filter(level -> level.getGender() == gender)
                .map(PartyLevel::getLevel)
                .toList();
    }
}
