package umc.cockple.demo.domain.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.chat.converter.ChatConverter;
import umc.cockple.demo.domain.chat.domain.ChatMessage;
import umc.cockple.demo.domain.chat.domain.ChatMessageImg;
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
        List<ChatCommonDTO.ImageInfo> processedImages = processMessageImages(message);
        boolean isMyMessage = isMyMessage(sender.getId(), memberId);
        boolean isSenderWithdrawn = sender.getIsActive() == MemberStatus.INACTIVE;
        return chatConverter.toCommonMessageInfo(message, senderProfileImageUrl, processedImages, isMyMessage, isSenderWithdrawn);
    }

    public String generateProfileImageUrl(ProfileImg profileImg) {
        if (profileImg != null && profileImg.getImgKey() != null && !profileImg.getImgKey().isBlank()) {
            return fileService.getUrlFromKey(profileImg.getImgKey());
        }
        return null;
    }

    private List<ChatCommonDTO.ImageInfo> processMessageImages(ChatMessage message) {
        return message.getChatMessageImgs().stream()
                .sorted(Comparator.comparing(ChatMessageImg::getImgOrder))
                .map(this::processSingleImage)
                .toList();
    }

    private ChatCommonDTO.ImageInfo processSingleImage(ChatMessageImg img) {
        String imageUrl = generateImageUrl(img);
        return chatConverter.toImageInfo(img, imageUrl);
    }

    public String generateImageUrl(ChatMessageImg img) {
        if (img != null && img.getImgKey() != null && !img.getImgKey().isBlank()) {
            return fileService.getUrlFromKey(img.getImgKey());
        }
        return null;
    }

    private boolean isMyMessage(Long senderId, Long currentUserId) {
        return senderId.equals(currentUserId);
    }

}
