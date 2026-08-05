package umc.cockple.demo.domain.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import umc.cockple.demo.domain.notification.command.NotificationCreateCommand;
import umc.cockple.demo.domain.notification.command.NotificationRequest;
import umc.cockple.demo.domain.notification.strategy.NotificationStrategyRegistry;

@Service
@RequiredArgsConstructor
public class NotificationIngressService {

    private final NotificationV2CommandService notificationV2CommandService;
    private final NotificationStrategyRegistry notificationStrategyRegistry;

    public void handle(Object event) {
        notificationStrategyRegistry.convert(event)
                .forEach(this::createNotification);
    }

    private void createNotification(NotificationRequest request) {
        notificationV2CommandService.createNotification(
                new NotificationCreateCommand(
                        request.recipientMemberId(),
                        request.title(),
                        request.content(),
                        request.imageKey(),
                        request.data(),
                        request.destination()
                ),
                request.legacyCompatibility()
        );
    }
}
