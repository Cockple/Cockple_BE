package umc.cockple.demo.domain.party.domain;

import jakarta.persistence.*;
import lombok.*;
import umc.cockple.demo.domain.party.enums.ActiveDay;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
public class PartyActiveDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(name = "party_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Party party;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ActiveDay activeDay;
}
