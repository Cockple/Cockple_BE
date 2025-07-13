package umc.cockple.demo.domain.contest.domain;

import jakarta.persistence.*;
import lombok.*;
import umc.cockple.demo.domain.contest.dto.ContestRecordCreateCommand;
import umc.cockple.demo.domain.contest.dto.ContestRecordUpdateRequestDTO;
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
    private Boolean contentIsOpen = true;

    @Column(nullable = false)
    private Boolean videoIsOpen = true;

    @OneToMany(mappedBy = "contest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ContestImg> contestImgs = new ArrayList<>();

    @OneToMany(mappedBy = "contest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ContestVideo> contestVideos = new ArrayList<>();


    public static Contest create(ContestRecordCreateCommand command, Member member) {
        return Contest.builder()
                .member(member)
                .contestName(command.contestName())
                .date(command.date())
                .medalType(command.medalType())
                .type(command.type())
                .level(command.level())
                .content(command.content())
                .contentIsOpen(command.contentIsOpen())
                .videoIsOpen(command.videoIsOpen())
                .contestImgs(new ArrayList<>()) // 이거 꼭!
                .contestVideos(new ArrayList<>())  // 안전하게 초기화
                .build();
    }

    public void addContestImg(ContestImg img) {
        this.contestImgs.add(img);
        img.setContest(this); // 양방향 유지
    }

    public void updateFromRequest(ContestRecordUpdateRequestDTO request) {
        this.contestName = request.contestName();
        this.date = request.date();
        this.medalType = request.medalType();
        this.type = request.type();
        this.level = request.level();
        this.content = request.content();
        this.contentIsOpen = request.contentIsOpen();
        this.videoIsOpen = request.videoIsOpen();
    }

}
