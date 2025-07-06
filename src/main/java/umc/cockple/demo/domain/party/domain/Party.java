package umc.cockple.demo.domain.party.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import umc.cockple.demo.enums.ActivityTime;
import umc.cockple.demo.enums.ParticipationType;
import umc.cockple.demo.global.common.BaseEntity;
import umc.cockple.demo.domain.member.domain.MemberParty;

import java.util.ArrayList;
import java.util.List;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
public class Party extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String partyName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_addr_id")
    private PartyAddr partyAddr;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ParticipationType partyType;

    @Column(nullable = false)
    private Long ownerId;

    private Integer minAge;

    private Integer maxAge;

    @Column(nullable = false)
    private Integer price;

    @Column(nullable = false)
    private Integer joinPrice;

    @Column(nullable = false)
    private String designatedCock;

    @ColumnDefault("0")
    @Column(nullable = false)
    private Integer exerciseCount;

    private String content;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ActivityTime activityTime;

    @OneToMany(mappedBy = "party", cascade = CascadeType.ALL)
    private List<PartyActiveDay> activeDays = new ArrayList<>();

    @OneToMany(mappedBy = "party", cascade = CascadeType.ALL)
    private List<PartyKeyword> keywords = new ArrayList<>();

    @OneToMany(mappedBy = "party", cascade = CascadeType.ALL)
    private List<PartyLevel> addresses = new ArrayList<>();

    @OneToMany(mappedBy = "party", cascade = CascadeType.ALL)
    private List<MemberParty> memberParties = new ArrayList<>();


}
