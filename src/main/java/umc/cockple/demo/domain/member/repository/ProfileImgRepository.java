package umc.cockple.demo.domain.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import umc.cockple.demo.domain.member.domain.ProfileImg;

import java.util.List;

public interface ProfileImgRepository extends JpaRepository<ProfileImg, Long> {

    @Modifying
    @Query("DELETE FROM ProfileImg pi WHERE pi.member.id IN :memberIds")
    void deleteByMemberIds(@Param("memberIds") List<Long> memberIds);
}
