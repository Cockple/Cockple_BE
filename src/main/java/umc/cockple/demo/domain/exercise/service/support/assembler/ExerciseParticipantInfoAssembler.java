package umc.cockple.demo.domain.exercise.service.support.assembler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.exercise.domain.Guest;
import umc.cockple.demo.domain.exercise.service.query.result.ExerciseDetailResult;
import umc.cockple.demo.domain.exercise.service.support.reader.MemberExerciseReader;
import umc.cockple.demo.domain.exercise.service.support.reader.GuestReader;
import umc.cockple.demo.domain.exercise.domain.MemberExercise;
import umc.cockple.demo.domain.file.service.ImageUrlResolver;
import umc.cockple.demo.domain.member.service.query.lookup.MemberLookupService;
import umc.cockple.demo.domain.member.service.query.lookup.MemberPartyLookupService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.ProfileImg;
import umc.cockple.demo.domain.member.enums.MemberStatus;
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

    private final MemberExerciseReader memberExerciseReader;
    private final GuestReader guestReader;
    private final MemberLookupService memberLookupService;
    private final MemberPartyLookupService memberPartyLookupService;
    private final ImageUrlResolver imageUrlResolver;

    public List<ExerciseDetailResult.ParticipantInfo> getAllSortedParticipants(Long exerciseId, Party party) {
        List<MemberExercise> memberExercises = memberExerciseReader.findMemberExercisesWithMemberAndProfile(exerciseId);
        List<ExerciseDetailResult.ParticipantInfo> memberParticipants = buildMemberParticipantInfos(memberExercises, party);

        List<Guest> guests = guestReader.findByExerciseId(exerciseId);
        List<ExerciseDetailResult.ParticipantInfo> guestParticipants = buildGuestParticipantInfos(guests);

        List<ExerciseDetailResult.ParticipantInfo> allParticipants = new ArrayList<>();
        allParticipants.addAll(memberParticipants);
        allParticipants.addAll(guestParticipants);

        allParticipants.sort(Comparator.comparing(ExerciseDetailResult.ParticipantInfo::joinedAt));

        return allParticipants;
    }

    private List<ExerciseDetailResult.ParticipantInfo> buildMemberParticipantInfos(
            List<MemberExercise> memberExercises, Party party) {
        if (memberExercises.isEmpty()) {
            return List.of();
        }

        List<Long> memberIds = memberExercises.stream()
                .map(me -> me.getMember().getId())
                .toList();

        Map<Long, Role> partyMemberRoles = memberPartyLookupService
                .findMemberRolesByPartyAndMembers(party.getId(), memberIds);

        return memberExercises.stream()
                .map(memberExercise -> toParticipantInfo(
                        memberExercise,
                        partyMemberRoles.get(memberExercise.getMember().getId())))
                .toList();
    }

    private List<ExerciseDetailResult.ParticipantInfo> buildGuestParticipantInfos(List<Guest> guests) {
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
                    return toParticipantInfo(guest, inviterName);
                })
                .toList();
    }

    private ExerciseDetailResult.ParticipantInfo toParticipantInfo(
            MemberExercise memberExercise, Role role) {
        Member member = memberExercise.getMember();

        return new ExerciseDetailResult.ParticipantInfo(
                member.getId(),
                0,
                imageUrlResolver.resolve(member.getProfileImg(), ProfileImg::getImgKey),
                member.getMemberName(),
                member.getGender().name(),
                member.getLevel().name(),
                memberExercise.getExerciseMemberShipStatus().name(),
                role != null ? role.name() : null,
                null,
                memberExercise.getCreatedAt(),
                member.getIsActive() == MemberStatus.INACTIVE
        );
    }

    private ExerciseDetailResult.ParticipantInfo toParticipantInfo(Guest guest, String inviterName) {
        return new ExerciseDetailResult.ParticipantInfo(
                guest.getId(),
                0,
                null,
                guest.getGuestName(),
                guest.getGender().name(),
                guest.getLevel().name(),
                guest.getExerciseMemberShipStatus().name(),
                null,
                inviterName,
                guest.getCreatedAt(),
                false
        );
    }
}
