package umc.cockple.demo.domain.exercise.domain;

import jakarta.persistence.*;
import lombok.*;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.global.common.BaseEntity;
import umc.cockple.demo.mapping.MemberExercise;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
public class Exercise extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(name = "addr_id")
    @OneToOne(fetch = FetchType.LAZY)
    private ExerciseAddr exerciseAddr;

    @JoinColumn(name = "party_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Party party;

    @Column(nullable = false)
    private LocalDate date; // 운동 날짜

    @Column(nullable = false)
    private LocalTime startTime;

    private LocalTime endTime;

    @Column(nullable = false)
    private Integer maxCapacity;

    @Column(nullable = false)
    private Integer nowCapacity;

    @Column(nullable = false)
    private Boolean partyGuestAccept;

    @Column(nullable = false)
    private Boolean outsideGuestAccept;

    private String notice;

    @OneToMany(mappedBy = "exercise", cascade = CascadeType.ALL)
    private List<Guest> addresses = new ArrayList<>();

    @OneToMany(mappedBy = "exercise", cascade = CascadeType.ALL)
    private List<MemberExercise> memberExercises = new ArrayList<>();

}
