package umc.cockple.demo.domain.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.cockple.demo.domain.chat.domain.DownloadToken;

public interface DownloadTokenRepository extends JpaRepository<DownloadToken, Long> {
}
