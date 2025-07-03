package umc.cockple.demo.domain.terms.domain;

import jakarta.persistence.*;
import lombok.*;
import umc.cockple.demo.mapping.MemberTerms;
import umc.cockple.demo.global.common.BaseEntity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
public class Terms extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private String version;

    @Column(nullable = false)
    private LocalDate effectiveDate;

    @Column(nullable = false)
    private Boolean inActive;

    @OneToMany(mappedBy = "terms", cascade = CascadeType.ALL)
    private List<MemberTerms> memberTerms = new ArrayList<>();
}
