package umc.cockple.demo.domain.party.domain;

import jakarta.persistence.*;
import lombok.*;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.global.common.BaseEntity;
import umc.cockple.demo.domain.party.enums.RequestStatus;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
public class PartyJoinRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id")
    private Party party;

    @Enumerated(EnumType.STRING)
    private RequestStatus status;

    public static PartyJoinRequest create(Member member, Party party) {
        return PartyJoinRequest.builder()
                .member(member)
                .party(party)
                .status(RequestStatus.PENDING)
                .build();
    }

    public void updateStatus(RequestStatus requestStatus) {
        this.status=requestStatus;
    }
}
