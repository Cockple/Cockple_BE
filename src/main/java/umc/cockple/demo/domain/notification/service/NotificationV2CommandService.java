package umc.cockple.demo.domain.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.exception.MemberException;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.notification.domain.Notification;
import umc.cockple.demo.domain.notification.domain.NotificationDestination;
import umc.cockple.demo.domain.notification.dto.NotificationCreateCommand;
import umc.cockple.demo.domain.notification.enums.NotificationResourceType;
import umc.cockple.demo.domain.notification.listener.event.NotificationEvent;
import umc.cockple.demo.domain.notification.exception.NotificationErrorCode;
import umc.cockple.demo.domain.notification.exception.NotificationException;
import umc.cockple.demo.domain.notification.repository.NotificationRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class NotificationV2CommandService {

    private static final int RETENTION_CAP = 50;
    private static final int RETENTION_TRIGGER = 60;
    private static final int INVITE_RETENTION_DAYS = 7;

    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;
    private final ApplicationEventPublisher eventPublisher;

    public void createNotification(NotificationCreateCommand command) {
        Member member = memberRepository.findById(command.recipientMemberId())
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        enforceRetentionPolicy(member);

        NotificationDestination destination = command.destination();
        Notification notification = Notification.builder()
                .member(member)
                .partyId(null)
                .resourceType(destination != null ? destination.resourceType() : null)
                .resourceId(destination != null ? destination.resourceId() : null)
                .action(destination != null ? destination.action() : null)
                .title(command.title())
                .content(command.content())
                .type(null)
                .isRead(false)
                .imageKey(command.imageKey())
                .data(command.data())
                .build();

        notificationRepository.save(notification);
        eventPublisher.publishEvent(new NotificationEvent(
                member.getId(), command.title(), command.content()));
    }

    public void markAsRead(Long memberId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.getMember().getId().equals(memberId)) {
            throw new NotificationException(NotificationErrorCode.NOTIFICATION_NOT_OWNED);
        }

        notification.read();
    }

    private void enforceRetentionPolicy(Member member) {
        long count = notificationRepository.countByMember(member);
        if (count < RETENTION_TRIGGER) {
            return;
        }

        int excess = (int) (count - RETENTION_CAP);
        List<Long> deletableIds = notificationRepository.findDeletableIdsOldestFirstByResourceType(
                member,
                NotificationResourceType.PARTY_INVITATION,
                LocalDateTime.now().minusDays(INVITE_RETENTION_DAYS),
                PageRequest.of(0, excess));

        if (!deletableIds.isEmpty()) {
            notificationRepository.deleteAllByIdInBatch(deletableIds);
        }
    }
}
