package umc.cockple.demo.domain.chat.domain;

import jakarta.persistence.*;
import lombok.*;
import umc.cockple.demo.global.common.BaseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class DownloadToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private Long fileId;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private LocalDateTime expiresAt;
}
