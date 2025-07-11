package umc.cockple.demo.domain.contest.domain;

import jakarta.persistence.*;
import lombok.*;

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

    public void setContest(Contest contest) {
        this.contest = contest;

        // 양방향 연관관계 유지
        if (!contest.getContestImgs().contains(this)) {
            contest.getContestImgs().add(this);
        }
    }

    public static ContestImg of(Contest contest, String imgUrl, String imgKey, int imgOrder) {
        return ContestImg.builder()
                .contest(contest)
                .imgUrl(imgUrl)
                .imgKey(imgKey)
                .imgOrder(imgOrder)
                .build();
    }
}
