package umc.cockple.demo.domain.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.chat.converter.ChatConverter;
import umc.cockple.demo.domain.chat.domain.ChatMessage;
import umc.cockple.demo.domain.chat.domain.ChatMessageFile;
import umc.cockple.demo.domain.chat.dto.ChatCommonDTO;
import umc.cockple.demo.domain.file.service.FileService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.ProfileImg;
import umc.cockple.demo.domain.member.enums.MemberStatus;

import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatProcessor {

    private final FileService fileService;
    private final ChatConverter chatConverter;

    public List<ChatCommonDTO.MessageInfo> processMessages(Long memberId, List<ChatMessage> recentMessages) {
        return recentMessages.stream()
                .map(message -> processAndConvertMessage(message, memberId))
                .toList();
    }

    private ChatCommonDTO.MessageInfo processAndConvertMessage(ChatMessage message, Long memberId) {
        Member sender = message.getSender();
        String senderProfileImageUrl = generateProfileImageUrl(sender.getProfileImg());
        List<ChatCommonDTO.FileInfo> processedFiles = processMessageFiles(message);
        boolean isMyMessage = isMyMessage(sender.getId(), memberId);
        boolean isSenderWithdrawn = sender.getIsActive() == MemberStatus.INACTIVE;
        return chatConverter.toCommonMessageInfo(message, senderProfileImageUrl, processedFiles, isMyMessage, isSenderWithdrawn);
    }

    public String generateProfileImageUrl(ProfileImg profileImg) {
        if (profileImg != null && profileImg.getImgKey() != null && !profileImg.getImgKey().isBlank()) {
            return fileService.getUrlFromKey(profileImg.getImgKey());
        }
        return null;
    }

    private List<ChatCommonDTO.FileInfo> processMessageFiles(ChatMessage message) {
        return message.getChatMessageFiles().stream()
                .sorted(Comparator.comparing(ChatMessageFile::getFileOrder))
                .map(this::processSingleFile)
                .toList();
    }

    private ChatCommonDTO.FileInfo processSingleFile(ChatMessageFile file) {
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
