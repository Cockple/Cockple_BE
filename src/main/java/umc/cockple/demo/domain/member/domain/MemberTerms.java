package umc.cockple.demo.domain.member.domain;

import jakarta.persistence.*;
import lombok.*;
import umc.cockple.demo.domain.terms.domain.Terms;
import umc.cockple.demo.global.common.BaseEntity;

import java.time.LocalDateTime;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
public class MemberTerms extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "terms_id")
    private Terms terms;

    @Column(nullable = false)
    private Boolean agree;

    @Column(nullable = false)
    private LocalDateTime agreedAt;
}
