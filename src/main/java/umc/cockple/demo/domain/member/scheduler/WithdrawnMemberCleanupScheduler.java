package umc.cockple.demo.domain.member.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.chat.repository.ChatMessageRepository;
import umc.cockple.demo.domain.file.service.FileService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.enums.MemberStatus;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.member.repository.MemberTermsRepository;
import umc.cockple.demo.domain.party.repository.PartyJoinRequestRepository;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class WithdrawnMemberCleanupScheduler {

    private final MemberRepository memberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final PartyJoinRequestRepository partyJoinRequestRepository;
    private final MemberTermsRepository memberTermsRepository;
    private final FileService fileService;

    // 매일 새벽 3시에 탈퇴 후 14일이 지난 회원 데이터 하드 딜리트
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void deleteExpiredWithdrawnMembers() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(14);
        List<Member> targets = memberRepository.findAllByIsActiveAndDeletedAtBefore(
                MemberStatus.INACTIVE, threshold);

        if (targets.isEmpty()) {
            return;
        }

        for (Member member : targets) {
            // S3 프로필 이미지 삭제
            if (member.getProfileImg() != null) {
                fileService.delete(member.getProfileImg().getImgKey());
            }
            // cascade 없는 FK 관계 수동 처리
            chatMessageRepository.nullifySenderByMemberId(member.getId()); // 메시지 보존, sender만 null
            partyJoinRequestRepository.deleteByMember(member);
            memberTermsRepository.deleteByMember(member);
        }

        memberRepository.deleteAll(targets);
        log.info("[CLEANUP] 탈퇴 회원 하드 딜리트 완료 - {}명", targets.size());
    }
}
