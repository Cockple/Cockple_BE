package umc.cockple.demo.domain.contest.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "contest_video")
@Getter
public class ContestVideo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contest_id")
    private Contest contest;

    @Column(nullable = false)
    private String videoUrl;

    @Setter
    @Column(nullable = false)
    private Integer videoOrder;

    public void setContest(Contest contest) {
        this.contest = contest;

        if (!contest.getContestVideos().contains(this)) {
            contest.getContestVideos().add(this);
        }
    }

    public static ContestVideo of(Contest contest, String videoUrl, Integer videoOrder) {
        ContestVideo video = ContestVideo.builder()
                .videoUrl(videoUrl)
                .videoOrder(videoOrder)
                .build();
        video.setContest(contest);
        return video;
    }

}
