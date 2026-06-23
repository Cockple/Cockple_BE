package umc.cockple.demo.domain.chat.service.websocket.session;

import java.util.Collection;
import java.util.List;

public interface ChatSessionRegistry {

    List<Long> findOpenMemberIds(Collection<Long> memberIds);
}
