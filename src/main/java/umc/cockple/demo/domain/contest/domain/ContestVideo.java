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

    @Column(nullable = false)
    private Integer videoOrder;

    public static ContestVideo of(Contest contest, String videoUrl, Integer videoOrder) {
        return ContestVideo.builder()
                .contest(contest)
                .videoUrl(videoUrl)
                .videoOrder(videoOrder)
                .build();
    }
}
