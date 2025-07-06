package umc.cockple.demo.mapping;

import jakarta.persistence.*;
import lombok.*;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.global.enums.PartyOrderType;
import umc.cockple.demo.global.enums.MemberPartyStatus;
import umc.cockple.demo.global.enums.Role;
import umc.cockple.demo.global.common.BaseEntity;

import java.time.LocalDateTime;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
public class MemberParty extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id")
    private Party party;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(nullable = false)
    private LocalDateTime joinedAt;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MemberPartyStatus status;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PartyOrderType orderType;

}
