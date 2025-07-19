package umc.cockple.demo.domain.exercise.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.exercise.converter.ExerciseConverter;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.domain.Guest;
import umc.cockple.demo.domain.exercise.dto.ExerciseDetailDTO;
import umc.cockple.demo.domain.exercise.dto.ExerciseDetailDTO.ParticipantInfo;
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.exception.ExerciseException;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.exercise.repository.GuestRepository;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberExercise;
import umc.cockple.demo.domain.member.domain.MemberParty;
import umc.cockple.demo.domain.member.repository.MemberExerciseRepository;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.global.enums.MemberStatus;
import umc.cockple.demo.global.enums.Role;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ExerciseQueryService {

    private final ExerciseRepository exerciseRepository;
    private final MemberRepository memberRepository;
    private final MemberPartyRepository memberPartyRepository;
    private final MemberExerciseRepository memberExerciseRepository;
    private final GuestRepository guestRepository;

    private final ExerciseConverter exerciseConverter;

    public ExerciseDetailDTO.Response getExerciseDetail(Long exerciseId, Long memberId) {

        log.info("운동 조회 시작 - exerciseId = {}, memberId = {}", exerciseId, memberId);

        Exercise exercise = findExerciseWithBasicInfoOrThrow(exerciseId);
        Member member = findMemberOrThrow(memberId);

        Party party = exercise.getParty();
        boolean isManager = checkManagerPermission(party, member);

        List<MemberExercise> memberExercises = findMemberExercisesWithMemberAndProfile(exerciseId);
        List<ParticipantInfo> memberParticipants = buildMemberParticipantInfos(memberExercises, party);

        List<Guest> guests = findGuests(exerciseId);
        List<ParticipantInfo> guestParticipants = buildGuestParticipantInfos(guests);


        return null;
    }

    // ========== 비즈니스 메서드 ==========

    private boolean checkManagerPermission(Party party, Member member) {
        return memberPartyRepository.existsByPartyIdAndMemberIdAndRole(
                party.getId(), member.getId(), Role.party_MANAGER);
    }

    private List<ParticipantInfo> buildMemberParticipantInfos(List<MemberExercise> memberExercises, Party party) {
        if (memberExercises.isEmpty()) {
            return List.of();
        }

        List<Long> memberIds = memberExercises.stream()
                .map(me -> me.getMember().getId())
                .toList();

        Map<Long, Role> partyMemberRoles = memberPartyRepository
                .findMemberRolesByPartyAndMembers(party.getId(), memberIds)
                .stream()
                .collect(Collectors.toMap(
                        mp -> mp.getMember().getId(),
                        MemberParty::getRole
                ));

        return memberExercises.stream()
                .map(me -> {
                    if (partyMemberRoles.containsKey(me.getMember().getId())) {
                        return exerciseConverter.toParticipantInfo(me, partyMemberRoles);
                    } else {
                        return exerciseConverter.toExeternalParticipantInfo(me);
                    }
                })
                .toList();
    }

    private List<ParticipantInfo> buildGuestParticipantInfos(List<Guest> guests) {
        if (guests.isEmpty()) {
            return List.of();
        }

        Set<Long> inviterIds = guests.stream()
                .map(Guest::getInviterId)
                .collect(Collectors.toSet());

        Map<Long, String> inviterNames = memberRepository.findMemberNamesByIds(inviterIds);

        return guests.stream()
                .map(guest -> {
                    String inviterName = inviterNames.getOrDefault(guest.getInviterId(), "알 수 없음");
                    return exerciseConverter.toParticipantInfo(guest, inviterName);
                })
                .toList();
    }

    // ========== 조회 메서드 ==========

    private Exercise findExerciseWithBasicInfoOrThrow(Long exerciseId) {
        return exerciseRepository.findExerciseWithBasicInfo(exerciseId)
                .orElseThrow(() -> new ExerciseException(ExerciseErrorCode.EXERCISE_NOT_FOUND));
    }

    private Member findMemberOrThrow(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new ExerciseException(ExerciseErrorCode.MEMBER_NOT_FOUND));
    }

    private List<MemberExercise> findMemberExercisesWithMemberAndProfile(Long exerciseId) {
        return memberExerciseRepository.findByExerciseIdWithMemberAndProfile(exerciseId, MemberStatus.ACTIVE);
    }

    private List<Guest> findGuests(Long exerciseId) {
        return guestRepository.findByExerciseId(exerciseId);
    }
}
