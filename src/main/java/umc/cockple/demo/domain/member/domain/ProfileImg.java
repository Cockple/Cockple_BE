package umc.cockple.demo.domain.member.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
public class ProfileImg {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", unique = true)
    private Member member;

    @Column(nullable = false)
    private String imgKey;

    // 동시 수정 시 lost update 방지를 위한 낙관적 락 버전
    @Version
    @Column(nullable = false)
    private Long version;


    public void setMember(Member member) {
        this.member = member;
    }

    // 프로필 사진 수정시 url만 변경
    public void updateProfile(String imgKey) {
        this.imgKey = imgKey;
    }

}
