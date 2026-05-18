package umc.cockple.demo.domain.member.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.bookmark.repository.ExerciseBookmarkRepository;
import umc.cockple.demo.domain.bookmark.repository.PartyBookmarkRepository;
import umc.cockple.demo.domain.chat.repository.ChatMessageRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.chat.repository.MessageReadStatusRepository;
import umc.cockple.demo.domain.contest.repository.ContestRepository;
import umc.cockple.demo.domain.file.service.FileService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.enums.MemberStatus;
import umc.cockple.demo.domain.member.repository.*;
import umc.cockple.demo.domain.notification.repository.NotificationRepository;
import umc.cockple.demo.domain.party.repository.PartyJoinRequestRepository;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class WithdrawnMemberCleanupScheduler {

    private final MemberRepository memberRepository;
    private final ProfileImgRepository profileImgRepository;
    private final MemberKeywordRepository memberKeywordRepository;
    private final MemberAddrRepository memberAddrRepository;
    private final MemberPartyRepository memberPartyRepository;
    private final MemberExerciseRepository memberExerciseRepository;
    private final MemberTermsRepository memberTermsRepository;
    private final ContestRepository contestRepository;
    private final NotificationRepository notificationRepository;
    private final ExerciseBookmarkRepository exerciseBookmarkRepository;
    private final PartyBookmarkRepository partyBookmarkRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MessageReadStatusRepository messageReadStatusRepository;
    private final PartyJoinRequestRepository partyJoinRequestRepository;
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

        List<Long> memberIds = targets.stream().map(Member::getId).toList();

        // S3 프로필 이미지 삭제 (DB 삭제 전에 처리)
        targets.stream()
                .filter(m -> m.getProfileImg() != null)
                .forEach(m -> fileService.delete(m.getProfileImg().getImgKey()));

        // Contest 자식 테이블 먼저 (FK: contest_id)
        contestRepository.deleteContestVideosByMemberIds(memberIds);
        contestRepository.deleteContestImgsByMemberIds(memberIds);
        contestRepository.deleteByMemberIds(memberIds);

        // sender_id null 처리 (메시지 보존)
        chatMessageRepository.nullifySenderByMemberIds(memberIds);

        // 나머지 자식 테이블 일괄 삭제
        messageReadStatusRepository.deleteByMemberIds(memberIds);
        partyJoinRequestRepository.deleteByMemberIds(memberIds);
        memberTermsRepository.deleteByMemberIds(memberIds);
        notificationRepository.deleteByMemberIds(memberIds);
        memberKeywordRepository.deleteByMemberIds(memberIds);
        memberAddrRepository.deleteByMemberIds(memberIds);
        chatRoomMemberRepository.deleteByMemberIds(memberIds);
        exerciseBookmarkRepository.deleteByMemberIds(memberIds);
        partyBookmarkRepository.deleteByMemberIds(memberIds);
        memberPartyRepository.deleteByMemberIds(memberIds);
        memberExerciseRepository.deleteByMemberIds(memberIds);
        profileImgRepository.deleteByMemberIds(memberIds);

        // Member 일괄 삭제
        memberRepository.deleteByMemberIds(memberIds);

        log.info("[CLEANUP] 탈퇴 회원 하드 딜리트 완료 - {}명", memberIds.size());
    }
}
