package umc.cockple.demo.domain.member.domain;

import jakarta.persistence.*;
import lombok.*;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.global.enums.PartyOrderType;
import umc.cockple.demo.global.enums.MemberPartyStatus;
import umc.cockple.demo.global.enums.Role;
import umc.cockple.demo.global.common.BaseEntity;

import java.time.LocalDateTime;

import static umc.cockple.demo.global.enums.MemberPartyStatus.ACTIVE;
import static umc.cockple.demo.global.enums.PartyOrderType.EARLIEST;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
public class MemberParty extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
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

    public static MemberParty createOwner(Member member, Party party) {
        return MemberParty.builder()
                .member(member)
                .party(party)
                .role(Role.party_MANAGER)
                .joinedAt(LocalDateTime.now())
                .orderType(EARLIEST)
                .status(ACTIVE)
                .build();
    }
}
