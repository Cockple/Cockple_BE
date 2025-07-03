package umc.cockple.demo.domain.contest.domain;

import jakarta.persistence.*;
import lombok.*;
import umc.cockple.demo.domain.member.domain.Member;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "contest_img")
@Getter
public class ContestImg {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contest_id")
    private Contest contest;

    @Column(nullable = false)
    private String imgUrl;

    @Column(nullable = false)
    private String imgKey;

    @Column(nullable = false)
    private Integer imgOrder;

}
