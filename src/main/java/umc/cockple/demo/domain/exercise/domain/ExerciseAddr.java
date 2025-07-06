package umc.cockple.demo.domain.exercise.domain;

import jakarta.persistence.*;
import lombok.*;
import umc.cockple.demo.global.common.BaseEntity;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
public class ExerciseAddr extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String addr1; // 도, 광역시

    @Column(nullable = false)
    private String addr2; // 시군구

    @Column(nullable = false)
    private String streetAddr;

    @Column(nullable = false)
    private String buildingName;

    @Column(nullable = false)
    private Float latitude;

    @Column(nullable = false)
    private Float longitude;

}
