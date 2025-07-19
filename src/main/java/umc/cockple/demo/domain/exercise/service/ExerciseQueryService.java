package umc.cockple.demo.domain.exercise.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.exercise.dto.ExerciseDetailDTO;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ExerciseQueryService {

    public ExerciseDetailDTO.Response getExerciseDetail(Long exerciseId, Long memberId) {

        return null;
    }
}
