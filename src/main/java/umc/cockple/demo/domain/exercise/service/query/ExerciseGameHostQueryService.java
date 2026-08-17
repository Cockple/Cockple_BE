package umc.cockple.demo.domain.exercise.service.query;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.service.ExerciseValidator;
import umc.cockple.demo.domain.exercise.service.query.result.ExerciseGameHostResult;
import umc.cockple.demo.domain.exercise.service.support.reader.ExerciseReader;
import umc.cockple.demo.domain.exercise.service.support.reader.MemberExerciseReader;
import umc.cockple.demo.domain.file.service.ImageUrlResolver;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberParty;
import umc.cockple.demo.domain.member.domain.ProfileImg;
import umc.cockple.demo.domain.member.service.query.lookup.MemberPartyLookupService;
import umc.cockple.demo.global.enums.Role;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ExerciseGameHostQueryService {

    private final ExerciseReader exerciseReader;
    private final MemberPartyLookupService memberPartyLookupService;
    private final MemberExerciseReader memberExerciseReader;
    private final ExerciseValidator exerciseValidator;
    private final ImageUrlResolver imageUrlResolver;

    public ExerciseGameHostResult getGameHost(Long exerciseId, Long memberId) {
        Exercise exercise = exerciseReader.findByIdOrThrow(exerciseId);
        exerciseValidator.validateExerciseManagementPermission(exercise, memberId);

        Long partyId = exercise.getParty().getId();
        List<MemberParty> memberParties = memberPartyLookupService
                .findActiveMembersWithProfile(partyId);
        List<Long> participantIds = memberParties.stream()
                .map(memberParty -> memberParty.getMember().getId())
                .toList();
        Map<Long, LocalDate> lastExerciseDates = memberExerciseReader
                .findLastExerciseDates(participantIds, partyId);

        List<ExerciseGameHostResult.Participant> participants = memberParties.stream()
                .sorted(gameHostCandidateOrder())
                .map(memberParty -> toParticipant(
                        memberParty,
                        exercise.getGameHostId(),
                        lastExerciseDates.get(memberParty.getMember().getId())))
                .toList();

        return new ExerciseGameHostResult(participants.size(), participants);
    }

    private Comparator<MemberParty> gameHostCandidateOrder() {
        return Comparator
                .comparingInt((MemberParty memberParty) -> rolePriority(memberParty.getRole()))
                .thenComparing(MemberParty::getJoinedAt)
                .thenComparing(memberParty -> memberParty.getMember().getId());
    }

    private int rolePriority(Role role) {
        return switch (role) {
            case PARTY_MANAGER -> 0;
            case PARTY_SUBMANAGER -> 1;
            case PARTY_MEMBER -> 2;
        };
    }

    private ExerciseGameHostResult.Participant toParticipant(
            MemberParty memberParty,
            Long gameHostId,
            LocalDate lastExerciseDate) {
        Member member = memberParty.getMember();

        return new ExerciseGameHostResult.Participant(
                member.getId(),
                imageUrlResolver.resolve(member.getProfileImg(), ProfileImg::getImgKey),
                memberParty.getRole(),
                member.getId().equals(gameHostId),
                member.getMemberName(),
                member.getGender(),
                member.getLevel(),
                lastExerciseDate
        );
    }
}
