package umc.cockple.demo.domain.contest.domain;

import jakarta.persistence.*;
import lombok.*;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.global.enums.MedalType;
import umc.cockple.demo.global.enums.ParticipationType;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.global.common.BaseEntity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
public class Contest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(nullable = false)
    private String contestName;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MedalType medalType;

    private LocalDate date; // 참가날짜

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ParticipationType type;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Level level;

    private String content;

    @Column(nullable = false)
    private Boolean contentIsOpen;

    @Column(nullable = false)
    private Boolean videoIsOpen;

    @OneToMany(mappedBy = "contest", cascade = CascadeType.ALL)
    private List<ContestImg> contestImgs = new ArrayList<>();

    @OneToMany(mappedBy = "contest", cascade = CascadeType.ALL)
    private List<ContestVideo> contestVideos = new ArrayList<>();
}
