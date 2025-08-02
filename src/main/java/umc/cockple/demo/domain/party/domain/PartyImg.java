package umc.cockple.demo.domain.party.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
public class PartyImg {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id")
    private Party party;

    @Column
    private String imgKey;

    public static PartyImg create(String imageKey, Party party) {
        return PartyImg.builder()
                .imgKey(imageKey)
                .party(party)
                .build();
    }
}
