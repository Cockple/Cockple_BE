package umc.cockple.demo.domain.chat.dto;

public class ChatFileDownloadDTO {

    public record Response(
            String originalFileName,
            String contentType,
            long contentLength,
            byte[] content
    ) {}
}
