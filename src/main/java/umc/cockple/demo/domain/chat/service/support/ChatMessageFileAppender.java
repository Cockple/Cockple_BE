package umc.cockple.demo.domain.chat.service.support;

import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.chat.domain.ChatMessage;
import umc.cockple.demo.domain.chat.domain.ChatMessageFile;
import umc.cockple.demo.domain.chat.dto.WebSocketMessageDTO;

import java.util.List;

@Component
public class ChatMessageFileAppender {

    public void append(ChatMessage message, List<WebSocketMessageDTO.Request.FileInfo> files) {
        if (files == null || files.isEmpty()) {
            return;
        }

        files.forEach(fileInfo -> {
            ChatMessageFile messageFile = ChatMessageFile.create(
                    message,
                    fileInfo.imgKey(),
                    fileInfo.imgOrder(),
                    fileInfo.originalFileName(),
                    fileInfo.fileSize(),
                    fileInfo.fileType()
            );
            message.getChatMessageFiles().add(messageFile);
        });
    }
}
