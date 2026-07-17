package umc.cockple.demo.domain.file.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageUrlResolverTest {

    @Mock
    private FileService fileService;

    @InjectMocks
    private ImageUrlResolver imageUrlResolver;

    @Test
    void resolveReturnsUrlFromExtractedImageKey() {
        TestImage image = new TestImage("party/image.png");
        when(fileService.getUrlFromKey("party/image.png")).thenReturn("https://example.com/party/image.png");

        String result = imageUrlResolver.resolve(image, TestImage::imgKey);

        assertThat(result).isEqualTo("https://example.com/party/image.png");
    }

    @Test
    void resolveReturnsNullWhenImageIsNull() {
        String result = imageUrlResolver.resolve(null, TestImage::imgKey);

        assertThat(result).isNull();
        verifyNoInteractions(fileService);
    }

    @Test
    void resolveReturnsNullWhenImageKeyIsBlank() {
        String result = imageUrlResolver.resolve(new TestImage(" "), TestImage::imgKey);

        assertThat(result).isNull();
        verifyNoInteractions(fileService);
    }

    private record TestImage(String imgKey) {
    }
}
