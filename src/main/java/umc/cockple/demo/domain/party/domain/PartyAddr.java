package umc.cockple.demo.domain.party.domain;

import jakarta.persistence.*;
import lombok.*;
import umc.cockple.demo.global.common.BaseEntity;

import java.util.ArrayList;
import java.util.List;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
public class PartyAddr extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String addr1; // 도, 광역시

    @Column(nullable = false)
    private String addr2; // 시군구

    @OneToMany(mappedBy = "partyAddr", cascade = CascadeType.ALL)
    private List<Party> parties = new ArrayList<>();

    public static PartyAddr create(String addr1, String addr2) {
        return PartyAddr.builder()
                .addr1(addr1)
                .addr2(addr2)
                .build();
    }
}
