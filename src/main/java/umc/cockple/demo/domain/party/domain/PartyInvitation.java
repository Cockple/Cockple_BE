package umc.cockple.demo.domain.party.domain;

import jakarta.persistence.*;
import lombok.*;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.party.enums.RequestStatus;
import umc.cockple.demo.global.common.BaseEntity;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
public class PartyInvitation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id")
    private Party party;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inviter_id") //초대한 사람
    private Member inviter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invitee_id") //초대받은 사람
    private Member invitee;

    @Enumerated(EnumType.STRING)
    private RequestStatus status;

    public static PartyInvitation create(Party party, Member inviter, Member invitee) {
        return PartyInvitation.builder()
                .party(party)
                .inviter(inviter)
                .invitee(invitee)
                .status(RequestStatus.PENDING)
                .build();
    }

    public void updateStatus(RequestStatus requestStatus) {
        this.status=requestStatus;
    }
}
