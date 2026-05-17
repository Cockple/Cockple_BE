package umc.cockple.demo.domain.notification.events;

import umc.cockple.demo.domain.member.domain.Member;

public record NotificationEvent(
        Member member,
        String title,
        String content
) {
}
