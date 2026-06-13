package umc.cockple.demo.domain.exercise.service.support.reader;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberExercise;
import umc.cockple.demo.domain.member.domain.MemberParty;
import umc.cockple.demo.domain.member.repository.MemberExerciseRepository;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.global.enums.Role;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ExerciseParticipantReader {

    private final ExerciseRepository exerciseRepository;
    private final MemberExerciseRepository memberExerciseRepository;
    private final MemberPartyRepository memberPartyRepository;

    public boolean hasManagerPermission(Party party, Member member) {
        return memberPartyRepository.existsByPartyIdAndMemberIdAndRole(
                party.getId(), member.getId(), Role.PARTY_MANAGER);
    }

    public boolean isPartyMember(Party party, Member member) {
        return memberPartyRepository.existsByPartyAndMember(party, member);
    }

    public List<Long> findPartyIdsByMemberId(Long memberId) {
        return memberPartyRepository.findPartyIdsByMemberId(memberId);
    }

    public List<MemberExercise> findMemberExercisesWithMemberAndProfile(Long exerciseId) {
        return memberExerciseRepository.findByExerciseIdWithMemberAndProfile(exerciseId);
    }

    public Map<Long, Role> findMemberRolesByPartyAndMembers(Long partyId, List<Long> memberIds) {
        return memberPartyRepository.findMemberRolesByPartyAndMembers(partyId, memberIds)
                .stream()
                .collect(Collectors.toMap(
                        memberParty -> memberParty.getMember().getId(),
                        MemberParty::getRole
                ));
    }

    public Map<Long, Integer> getParticipantCountsMap(Long partyId, LocalDate start, LocalDate end) {
        List<Object[]> countResults = exerciseRepository.findExerciseParticipantCounts(partyId, start, end);
        return toCountMap(countResults);
    }

    public Map<Long, Integer> getParticipantCountsMap(List<Long> exerciseIds, LocalDate start, LocalDate end) {
        if (exerciseIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Object[]> countResults = exerciseRepository.findExerciseParticipantCountsByExerciseIds(
                exerciseIds, start, end);
        return toCountMap(countResults);
    }

    public Map<Long, Integer> getParticipantCountsMap(List<Long> exerciseIds) {
        if (exerciseIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Object[]> countResults = exerciseRepository.findExerciseParticipantCountsByExerciseIds(exerciseIds);
        return toCountMap(countResults);
    }

    public Map<Long, Boolean> getParticipatingStatus(Long memberId, List<Long> exerciseIds) {
        if (exerciseIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> participatingExerciseIds = memberExerciseRepository
                .findAllExerciseIdsByMemberAndExerciseIds(memberId, exerciseIds);

        return exerciseIds.stream()
                .collect(Collectors.toMap(
                        exerciseId -> exerciseId,
                        participatingExerciseIds::contains
                ));
    }

    private Map<Long, Integer> toCountMap(List<Object[]> countResults) {
        return countResults.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).intValue()
                ));
    }
}
