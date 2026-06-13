package umc.cockple.demo.domain.exercise.service.support;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.exercise.converter.ExerciseConverter;
import umc.cockple.demo.domain.exercise.domain.Guest;
import umc.cockple.demo.domain.exercise.dto.ExerciseDetailDTO;
import umc.cockple.demo.domain.exercise.service.support.reader.ExerciseParticipantReader;
import umc.cockple.demo.domain.exercise.service.support.reader.GuestReader;
import umc.cockple.demo.domain.member.domain.MemberExercise;
import umc.cockple.demo.domain.member.service.support.MemberLookupService;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.global.enums.Role;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ExerciseParticipantInfoAssembler {

    private final ExerciseParticipantReader exerciseParticipantReader;
    private final GuestReader guestReader;
    private final MemberLookupService memberLookupService;
    private final ExerciseConverter exerciseConverter;

    public List<ExerciseDetailDTO.ParticipantInfo> getAllSortedParticipants(Long exerciseId, Party party) {
        List<MemberExercise> memberExercises = exerciseParticipantReader.findMemberExercisesWithMemberAndProfile(exerciseId);
        List<ExerciseDetailDTO.ParticipantInfo> memberParticipants = buildMemberParticipantInfos(memberExercises, party);

        List<Guest> guests = guestReader.findByExerciseId(exerciseId);
        List<ExerciseDetailDTO.ParticipantInfo> guestParticipants = buildGuestParticipantInfos(guests);

        List<ExerciseDetailDTO.ParticipantInfo> allParticipants = new ArrayList<>();
        allParticipants.addAll(memberParticipants);
        allParticipants.addAll(guestParticipants);

        allParticipants.sort(Comparator.comparing(ExerciseDetailDTO.ParticipantInfo::joinedAt));

        return allParticipants;
    }

    private List<ExerciseDetailDTO.ParticipantInfo> buildMemberParticipantInfos(List<MemberExercise> memberExercises, Party party) {
        if (memberExercises.isEmpty()) {
            return List.of();
        }

        List<Long> memberIds = memberExercises.stream()
                .map(me -> me.getMember().getId())
                .toList();

        Map<Long, Role> partyMemberRoles = exerciseParticipantReader
                .findMemberRolesByPartyAndMembers(party.getId(), memberIds);

        return memberExercises.stream()
                .map(me -> {
                    if (partyMemberRoles.containsKey(me.getMember().getId())) {
                        return exerciseConverter.toParticipantInfoFromMember(me, partyMemberRoles);
                    } else {
                        return exerciseConverter.toParticipantInfoFromExternalMember(me);
                    }
                })
                .toList();
    }

    private List<ExerciseDetailDTO.ParticipantInfo> buildGuestParticipantInfos(List<Guest> guests) {
        if (guests.isEmpty()) {
            return List.of();
        }

        Set<Long> inviterIds = guests.stream()
                .map(Guest::getInviterId)
                .collect(Collectors.toSet());

        Map<Long, String> inviterNames = memberLookupService.findNamesByIds(inviterIds);

        return guests.stream()
                .map(guest -> {
                    String inviterName = inviterNames.getOrDefault(guest.getInviterId(), "알 수 없음");
                    return exerciseConverter.toParticipantInfoFromGuest(guest, inviterName);
                })
                .toList();
    }
}
