package umc.cockple.demo.domain.exercise.converter.query;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.exercise.domain.Guest;
import umc.cockple.demo.domain.exercise.dto.lifecycle.ExerciseDetailDTO;
import umc.cockple.demo.domain.file.service.ImageUrlResolver;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberExercise;
import umc.cockple.demo.domain.member.domain.ProfileImg;
import umc.cockple.demo.domain.member.enums.MemberStatus;
import umc.cockple.demo.global.enums.Role;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class ExerciseParticipantInfoQueryMapper {

    private final ImageUrlResolver imageUrlResolver;

    public ExerciseDetailDTO.ParticipantInfo toParticipantInfoFromMember(MemberExercise memberParticipant, Map<Long, Role> memberRoles) {
        Member member = memberParticipant.getMember();
        Role role = memberRoles.get(member.getId());

        return ExerciseDetailDTO.ParticipantInfo.builder()
                .participantId(member.getId())
                .participantNumber(0)
                .profileImageUrl(imageUrlResolver.resolve(member.getProfileImg(), ProfileImg::getImgKey))
                .name(member.getMemberName())
                .gender(member.getGender().name())
                .level(member.getLevel().name())
                .participantType(memberParticipant.getExerciseMemberShipStatus().name())
                .partyPosition(role.name())
                .inviterName(null)
                .joinedAt(memberParticipant.getCreatedAt())
                .isWithdrawn(member.getIsActive() == MemberStatus.INACTIVE)
                .build();
    }

    public ExerciseDetailDTO.ParticipantInfo toParticipantInfoFromExternalMember(MemberExercise memberParticipant) {
        Member member = memberParticipant.getMember();

        return ExerciseDetailDTO.ParticipantInfo.builder()
                .participantId(member.getId())
                .participantNumber(0)
                .profileImageUrl(imageUrlResolver.resolve(member.getProfileImg(), ProfileImg::getImgKey))
                .name(member.getMemberName())
                .gender(member.getGender().name())
                .level(member.getLevel().name())
                .participantType(memberParticipant.getExerciseMemberShipStatus().name())
                .partyPosition(null)
                .inviterName(null)
                .joinedAt(memberParticipant.getCreatedAt())
                .isWithdrawn(member.getIsActive() == MemberStatus.INACTIVE)
                .build();
    }

    public ExerciseDetailDTO.ParticipantInfo toParticipantInfoFromGuest(Guest guest, String inviterName) {

        return ExerciseDetailDTO.ParticipantInfo.builder()
                .participantId(guest.getId())
                .participantNumber(0)
                .profileImageUrl(null)
                .name(guest.getGuestName())
                .gender(guest.getGender().name())
                .level(guest.getLevel().name())
                .participantType(guest.getExerciseMemberShipStatus().name())
                .partyPosition(null)
                .inviterName(inviterName)
                .joinedAt(guest.getCreatedAt())
                .isWithdrawn(false)
                .build();
    }
}
