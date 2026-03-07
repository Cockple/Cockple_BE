package umc.cockple.demo.domain.file.integration;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import umc.cockple.demo.domain.file.dto.FileUploadDTO;
import umc.cockple.demo.domain.file.service.FileService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.global.enums.DomainType;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.IntegrationTestBase;
import umc.cockple.demo.support.SecurityContextHelper;
import umc.cockple.demo.support.fixture.MemberFixture;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("File 통합 테스트")
class FileIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @MockitoBean
    private FileService fileService;

    private Member member;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(MemberFixture.createMember("홍길동", Gender.MALE, Level.A, 1001L));
    }

    @AfterEach
    void tearDown() {
        memberRepository.deleteAll();
        SecurityContextHelper.clearAuthentication();
    }

    @Nested
    @DisplayName("POST /api/gcs/upload/file - 단일 파일 업로드")
    class UploadFile {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("200 - 정상적인 File과 DomainType이 주어지면 업로드 성공 응답을 반환한다")
            void success_uploadSingleFile() throws Exception {
                // 가상 파일 생성
                MockMultipartFile mockFile = new MockMultipartFile(
                        "file",
                        "test.jpg",
                        MediaType.IMAGE_JPEG_VALUE,
                        "test image content".getBytes()
                );

                // 가상 응답 객체 생성
                FileUploadDTO.Response mockResponse = FileUploadDTO.Response.builder()
                        .fileKey("chat/test-key.jpg")
                        .fileUrl("https://storage.googleapis.com/test-bucket/chat/test-key.jpg")
                        .originalFileName("test.jpg")
                        .fileSize(18L)
                        .fileType(MediaType.IMAGE_JPEG_VALUE)
                        .build();

                given(fileService.uploadFile(any(), eq(DomainType.CHAT))).willReturn(mockResponse);
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(multipart("/api/gcs/upload/file")
                                .file(mockFile)
                                .param("domainType", "CHAT")
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.fileKey").value("chat/test-key.jpg"))
                        .andExpect(jsonPath("$.data.fileUrl").value("https://storage.googleapis.com/test-bucket/chat/test-key.jpg"))
                        .andExpect(jsonPath("$.data.originalFileName").value("test.jpg"));
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("400 - 파일 파라미터가 누락되면 실패 응답을 반환한다")
            void fail_missingFileParam() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(multipart("/api/gcs/upload/file")
                                .param("domainType", "CHAT")
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                        .andExpect(status().isBadRequest());
            }
        }
    }

    @Nested
    @DisplayName("POST /api/gcs/upload/files - 다중 파일 업로드")
    class UploadFiles {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("202 - 정상적인 파일 목록과 DomainType이 주어지면 업로드 성공 리스트를 반환한다")
            void success_uploadMultipleFiles() throws Exception {
                MockMultipartFile mockFile1 = new MockMultipartFile("file", "test1.jpg", MediaType.IMAGE_JPEG_VALUE, "content1".getBytes());
                MockMultipartFile mockFile2 = new MockMultipartFile("file", "test2.png", MediaType.IMAGE_PNG_VALUE, "content2".getBytes());

                FileUploadDTO.Response mockResponse1 = FileUploadDTO.Response.builder()
                        .fileKey("party/key1.jpg")
                        .fileUrl("https://url.com/party/key1.jpg")
                        .originalFileName("test1.jpg")
                        .fileSize(8L)
                        .fileType(MediaType.IMAGE_JPEG_VALUE)
                        .build();

                FileUploadDTO.Response mockResponse2 = FileUploadDTO.Response.builder()
                        .fileKey("party/key2.png")
                        .fileUrl("https://url.com/party/key2.png")
                        .originalFileName("test2.png")
                        .fileSize(8L)
                        .fileType(MediaType.IMAGE_PNG_VALUE)
                        .build();

                given(fileService.uploadFiles(any(), eq(DomainType.PARTY))).willReturn(List.of(mockResponse1, mockResponse2));
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(multipart("/api/gcs/upload/files")
                                .file(mockFile1)
                                .file(mockFile2)
                                .param("domainType", "PARTY")
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data[0].fileKey").value("party/key1.jpg"))
                        .andExpect(jsonPath("$.data[1].fileKey").value("party/key2.png"));
            }
        }
    }
}
