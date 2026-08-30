package umc.cockple.demo.domain.exercise.service.support.assembler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.exercise.domain.Guest;
import umc.cockple.demo.domain.exercise.service.query.model.ExerciseParticipantSnapshot;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ExerciseParticipantSnapshotAssembler {

    private final MemberExerciseReader memberExerciseReader;
    private final GuestReader guestReader;
    private final MemberLookupService memberLookupService;
    private final MemberPartyLookupService memberPartyLookupService;
    private final ImageUrlResolver imageUrlResolver;

    public List<ExerciseParticipantSnapshot> getAllParticipants(Long exerciseId, Party party) {
        List<MemberExercise> memberExercises = memberExerciseReader.findMemberExercisesWithMemberAndProfile(exerciseId);
        List<ExerciseParticipantSnapshot> memberParticipants = buildMemberParticipantSnapshots(memberExercises, party);

        List<Guest> guests = guestReader.findByExerciseId(exerciseId);
        List<ExerciseParticipantSnapshot> guestParticipants = buildGuestParticipantSnapshots(guests);

        List<ExerciseParticipantSnapshot> allParticipants = new ArrayList<>();
        allParticipants.addAll(memberParticipants);
        allParticipants.addAll(guestParticipants);

        return allParticipants;
    }

    private List<ExerciseParticipantSnapshot> buildMemberParticipantSnapshots(
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
                .map(memberExercise -> toParticipantSnapshot(
                        memberExercise,
                        partyMemberRoles.get(memberExercise.getMember().getId())))
                .toList();
    }

    private List<ExerciseParticipantSnapshot> buildGuestParticipantSnapshots(List<Guest> guests) {
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
                    return toParticipantSnapshot(guest, inviterName);
                })
                .toList();
    }

    private ExerciseParticipantSnapshot toParticipantSnapshot(
            MemberExercise memberExercise, Role role) {
        Member member = memberExercise.getMember();

        return new ExerciseParticipantSnapshot(
                member.getId(),
                imageUrlResolver.resolve(member.getProfileImg(), ProfileImg::getImgKey),
                member.getMemberName(),
                member.getGender(),
                member.getLevel(),
                memberExercise.getExerciseMemberShipStatus(),
                role,
                null,
                memberExercise.getCreatedAt(),
                member.getIsActive() == MemberStatus.INACTIVE
        );
    }

    private ExerciseParticipantSnapshot toParticipantSnapshot(Guest guest, String inviterName) {
        return new ExerciseParticipantSnapshot(
                guest.getId(),
                null,
                guest.getGuestName(),
                guest.getGender(),
                guest.getLevel(),
                guest.getExerciseMemberShipStatus(),
                null,
                inviterName,
                guest.getCreatedAt(),
                false
        );
    }
}
