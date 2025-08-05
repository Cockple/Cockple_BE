package umc.cockple.demo.domain.exercise.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import umc.cockple.demo.domain.exercise.dto.ExerciseCreateDTO;
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.exception.ExerciseException;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.enums.PartyStatus;
import umc.cockple.demo.domain.party.exception.PartyErrorCode;
import umc.cockple.demo.domain.party.exception.PartyException;
import umc.cockple.demo.global.enums.Role;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExerciseValidator {

    private final MemberPartyRepository memberPartyRepository;

    public void validateCreateExercise(Long memberId, ExerciseCreateDTO.Request request, Party party) {
        validatePartyIsActive(party);
        validateMemberPermission(memberId, party);
        validateExerciseTime(request);
    }

    // ========== 세부 검증 메서드들 ==========

    private void validatePartyIsActive(Party party) {
        if (party.getStatus() == PartyStatus.INACTIVE) {
            throw new PartyException(PartyErrorCode.PARTY_IS_DELETED);
        }
    }

    private void validateMemberPermission(Long memberId, Party party) {
        boolean isOwner = party.getOwnerId().equals(memberId);
        boolean isManager = memberPartyRepository.existsByPartyIdAndMemberIdAndRole(
                party.getId(), memberId, Role.party_MANAGER);

        if (!isOwner && !isManager)
            throw new ExerciseException(ExerciseErrorCode.INSUFFICIENT_PERMISSION);
    }

    private void validateExerciseTime(ExerciseCreateDTO.Request request) {
        LocalDate date = request.toParsedDate();
        LocalTime startTime = request.toParsedStartTime();
        LocalTime endTime = request.toParsedEndTime();

        if (!startTime.isBefore(endTime)) {
            throw new ExerciseException(ExerciseErrorCode.INVALID_EXERCISE_TIME);
        }

        LocalDateTime exerciseDateTime = LocalDateTime.of(date, startTime);
        if (exerciseDateTime.isBefore(LocalDateTime.now())) {
            throw new ExerciseException(ExerciseErrorCode.PAST_TIME_NOT_ALLOWED);
        }
    }

}
