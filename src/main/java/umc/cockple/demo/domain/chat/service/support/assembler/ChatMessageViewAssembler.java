package umc.cockple.demo.domain.chat.service.support.assembler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.chat.converter.ChatConverter;
import umc.cockple.demo.domain.chat.domain.ChatMessage;
import umc.cockple.demo.domain.chat.domain.ChatMessageFile;
import umc.cockple.demo.domain.chat.dto.ChatCommonDTO;
import umc.cockple.demo.domain.chat.enums.MessageType;
import umc.cockple.demo.domain.file.service.FileService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.ProfileImg;

import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ChatMessageViewAssembler {

    private final FileService fileService;
    private final ChatConverter chatConverter;

    public List<ChatCommonDTO.MessageInfo> assembleMessages(Long memberId, List<ChatMessage> recentMessages) {
        return recentMessages.stream()
                .map(message -> assembleMessageInfo(message, memberId))
                .toList();
    }

    private ChatCommonDTO.MessageInfo assembleMessageInfo(ChatMessage message, Long memberId) {
        Member sender = message.getSender();
        boolean isSystemMessage = message.getType() == MessageType.SYSTEM;
        boolean isSenderWithdrawn = !isSystemMessage && (sender == null || sender.isWithdrawn());
        String senderProfileImageUrl = sender != null && !isSenderWithdrawn
                ? generateProfileImageUrl(sender.getProfileImg())
                : null;
        List<ChatCommonDTO.FileInfo> fileInfos = assembleSortedFileInfos(message);
        boolean isMyMessage = sender != null && !isSenderWithdrawn && isMyMessage(sender.getId(), memberId);
        return chatConverter.toCommonMessageInfo(message, senderProfileImageUrl, fileInfos, isMyMessage, isSenderWithdrawn);
    }

    public String generateProfileImageUrl(ProfileImg profileImg) {
        if (profileImg != null && profileImg.getImgKey() != null && !profileImg.getImgKey().isBlank()) {
            return fileService.getUrlFromKey(profileImg.getImgKey());
        }
        return null;
    }

    private List<ChatCommonDTO.FileInfo> assembleSortedFileInfos(ChatMessage message) {
        return message.getChatMessageFiles().stream()
                .sorted(Comparator.comparing(ChatMessageFile::getFileOrder))
                .map(this::assembleFileInfo)
                .toList();
    }

    private ChatCommonDTO.FileInfo assembleFileInfo(ChatMessageFile file) {
        String imageUrl = generateFileUrl(file);
        return chatConverter.toFileInfo(file, imageUrl);
    }

    public String generateFileUrl(ChatMessageFile file) {
        if (file != null && file.getFileKey() != null && !file.getFileKey().isBlank()) {
            return fileService.getUrlFromKey(file.getFileKey());
        }
        return null;
    }

    private boolean isMyMessage(Long senderId, Long currentUserId) {
        return senderId.equals(currentUserId);
    }

}
