package umc.cockple.demo.domain.exercise.service.support.assembler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.exercise.domain.Guest;
import umc.cockple.demo.domain.exercise.service.query.model.ExerciseParticipantSnapshot;
import umc.cockple.demo.domain.exercise.service.support.reader.ExerciseParticipationReader;
import umc.cockple.demo.domain.exercise.service.support.reader.GuestReader;
import umc.cockple.demo.domain.exercise.domain.ExerciseParticipation;
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

    private final ExerciseParticipationReader exerciseParticipationReader;
    private final GuestReader guestReader;
    private final MemberLookupService memberLookupService;
    private final MemberPartyLookupService memberPartyLookupService;
    private final ImageUrlResolver imageUrlResolver;

    public List<ExerciseParticipantSnapshot> getAllParticipants(Long exerciseId, Party party) {
        List<ExerciseParticipation> participations = exerciseParticipationReader.findExerciseParticipationsWithMemberAndProfile(exerciseId);
        List<ExerciseParticipantSnapshot> memberParticipants = buildMemberParticipantSnapshots(participations, party);

        List<Guest> guests = guestReader.findByExerciseId(exerciseId);
        List<ExerciseParticipantSnapshot> guestParticipants = buildGuestParticipantSnapshots(guests);

        List<ExerciseParticipantSnapshot> allParticipants = new ArrayList<>();
        allParticipants.addAll(memberParticipants);
        allParticipants.addAll(guestParticipants);

        return allParticipants;
    }

    private List<ExerciseParticipantSnapshot> buildMemberParticipantSnapshots(
            List<ExerciseParticipation> participations, Party party) {
        if (participations.isEmpty()) {
            return List.of();
        }

        List<Long> memberIds = participations.stream()
                .map(me -> me.getMember().getId())
                .toList();

        Map<Long, Role> partyMemberRoles = memberPartyLookupService
                .findMemberRolesByPartyAndMembers(party.getId(), memberIds);

        return participations.stream()
                .map(exerciseParticipation -> toParticipantSnapshot(
                        exerciseParticipation,
                        partyMemberRoles.get(exerciseParticipation.getMember().getId())))
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
            ExerciseParticipation exerciseParticipation, Role role) {
        Member member = exerciseParticipation.getMember();

        return new ExerciseParticipantSnapshot(
                member.getId(),
                imageUrlResolver.resolve(member.getProfileImg(), ProfileImg::getImgKey),
                member.getDisplayName(),
                member.getGender(),
                member.getLevel(),
                exerciseParticipation.getExerciseMemberShipStatus(),
                role,
                null,
                exerciseParticipation.getCreatedAt(),
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
