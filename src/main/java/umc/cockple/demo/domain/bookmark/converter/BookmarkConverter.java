package umc.cockple.demo.domain.bookmark.converter;

import umc.cockple.demo.domain.bookmark.domain.ExerciseBookmark;
import umc.cockple.demo.domain.bookmark.dto.GetAllExerciseBookmarksResponseDTO;

public class BookmarkConverter {

    public static GetAllExerciseBookmarksResponseDTO ExerciseBookmarkToDTO(ExerciseBookmark bookmark) {
        return GetAllExerciseBookmarksResponseDTO.builder()
                .exerciseId(bookmark.getExercise().getId())
                // 추가
                .build();
    }
}
