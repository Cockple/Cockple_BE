package umc.cockple.demo.domain.member.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberAddr;
import umc.cockple.demo.domain.member.dto.CreateMemberAddrDTO;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.repository.MemberAddrRepository;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.domain.PartyAddr;
import umc.cockple.demo.domain.party.repository.PartyAddrRepository;
import umc.cockple.demo.domain.party.repository.PartyRepository;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.global.enums.Role;
import umc.cockple.demo.global.oauth2.service.KakaoOauthService;
import umc.cockple.demo.support.IntegrationTestBase;
import umc.cockple.demo.support.SecurityContextHelper;
import umc.cockple.demo.support.fixture.MemberAddrFixture;
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MemberIntegrationTest extends IntegrationTestBase {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired MemberRepository memberRepository;
    @Autowired MemberAddrRepository memberAddrRepository;
    @Autowired MemberPartyRepository memberPartyRepository;
    @Autowired PartyRepository partyRepository;
    @Autowired PartyAddrRepository partyAddrRepository;

    // withdrawMember에서 카카오 연결 끊기 API 호출을 막기 위해 Mock 처리
    @MockitoBean
    KakaoOauthService kakaoOauthService;

    private Member member;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(MemberFixture.createMember("홍길동", Gender.MALE, Level.A, 1001L));
    }

    @AfterEach
    void tearDown() {
        memberPartyRepository.deleteAll();
        partyRepository.deleteAll();
        partyAddrRepository.deleteAll();
        memberRepository.deleteAll(); // cascade: MemberAddr, MemberKeyword 등 함께 삭제
        SecurityContextHelper.clearAuthentication();
    }


    @Nested
    @DisplayName("PATCH /api/member - 회원 탈퇴")
    class WithdrawMember {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("200 - 일반 멤버가 탈퇴하면 성공한다")
            void normalMember_withdrawSuccess() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(patch("/api/member"))
                        .andExpect(status().isOk());
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("400 - 모임장은 탈퇴할 수 없다")
            void manager_cannotWithdraw() throws Exception {
                PartyAddr addr = partyAddrRepository.save(PartyFixture.createPartyAddr("서울특별시", "강남구"));
                Party party = partyRepository.save(PartyFixture.createParty("테스트 모임", member.getId(), addr));
                memberPartyRepository.save(MemberFixture.createMemberParty(party, member, Role.party_MANAGER));

                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(patch("/api/member"))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(MemberErrorCode.MANAGER_CANNOT_LEAVE.getCode()))
                        .andExpect(jsonPath("$.message").value(MemberErrorCode.MANAGER_CANNOT_LEAVE.getMessage()));
            }

            @Test
            @DisplayName("400 - 부모임장은 탈퇴할 수 없다")
            void subManager_cannotWithdraw() throws Exception {
                PartyAddr addr = partyAddrRepository.save(PartyFixture.createPartyAddr("서울특별시", "강남구"));
                Party party = partyRepository.save(PartyFixture.createParty("테스트 모임", member.getId(), addr));
                memberPartyRepository.save(MemberFixture.createMemberParty(party, member, Role.party_SUBMANAGER));

                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(patch("/api/member"))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(MemberErrorCode.SUBMANAGER_CANNOT_LEAVE.getCode()))
                        .andExpect(jsonPath("$.message").value(MemberErrorCode.SUBMANAGER_CANNOT_LEAVE.getMessage()));
            }
        }
    }


    @Nested
    @DisplayName("GET /api/profile/{memberId} - 타인 프로필 조회")
    class GetProfile {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("200 - 프로필 조회 시 memberName, gender, level이 반환된다")
            void getProfile_success() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/profile/{memberId}", member.getId()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.memberName").value("홍길동"))
                        .andExpect(jsonPath("$.data.gender").value("MALE"))
                        .andExpect(jsonPath("$.data.level").value("A"));
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("404 - 존재하지 않는 멤버 조회 시 MEMBER_NOT_FOUND 에러를 반환한다")
            void memberNotFound() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/profile/{memberId}", 999L))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value(MemberErrorCode.MEMBER_NOT_FOUND.getCode()))
                        .andExpect(jsonPath("$.message").value(MemberErrorCode.MEMBER_NOT_FOUND.getMessage()));
            }
        }
    }


    @Nested
    @DisplayName("GET /api/my/profile - 내 프로필 조회")
    class GetMyProfile {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("200 - 대표 주소가 있으면 내 프로필이 반환된다")
            void getMyProfile_success() throws Exception {
                memberAddrRepository.save(MemberAddrFixture.createMainAddr(member));
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/my/profile"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.memberName").value("홍길동"))
                        .andExpect(jsonPath("$.data.addr3").value("역삼동"))
                        .andExpect(jsonPath("$.data.myExerciseCnt").value(0));
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("400 - 대표 주소가 없으면 MAIN_ADDRESS_NULL 에러를 반환한다")
            void noMainAddress_fail() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/my/profile"))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(MemberErrorCode.MAIN_ADDRESS_NULL.getCode()))
                        .andExpect(jsonPath("$.message").value(MemberErrorCode.MAIN_ADDRESS_NULL.getMessage()));
            }
        }
    }


    @Nested
    @DisplayName("POST /api/my/profile/locations - 주소 추가")
    class AddAddress {

        private CreateMemberAddrDTO.CreateMemberAddrRequestDTO validRequest;

        @BeforeEach
        void setUp() {
            validRequest = new CreateMemberAddrDTO.CreateMemberAddrRequestDTO(
                    "서울특별시", "강남구", "역삼동", "테헤란로 123", "ㅁㅁ빌딩", 37.5, 127.0);
        }

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("200 - 새 주소를 추가하면 memberAddrId를 반환한다")
            void addAddress_success() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(post("/api/my/profile/locations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.memberAddrId").isNumber());
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("400 - 같은 주소를 중복 추가하면 DUPLICATE_ADDRESS 에러를 반환한다")
            void duplicateAddress_fail() throws Exception {
                memberAddrRepository.save(MemberAddrFixture.createMainAddr(member)); // validRequest와 동일한 값
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(post("/api/my/profile/locations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(MemberErrorCode.DUPLICATE_ADDRESS.getCode()))
                        .andExpect(jsonPath("$.message").value(MemberErrorCode.DUPLICATE_ADDRESS.getMessage()));
            }

            @Test
            @DisplayName("400 - 주소가 이미 5개면 OVER_NUMBER_OF_ADDR 에러를 반환한다")
            void overNumberOfAddr_fail() throws Exception {
                // validRequest(강남구)와 addr2가 달라 중복 검사를 통과하도록 마포구 주소 5개 생성
                for (int i = 1; i <= 5; i++) {
                    memberAddrRepository.save(MemberAddrFixture.createAddr(member, "동" + i, "길로 " + i, i == 1));
                }
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(post("/api/my/profile/locations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(MemberErrorCode.OVER_NUMBER_OF_ADDR.getCode()))
                        .andExpect(jsonPath("$.message").value(MemberErrorCode.OVER_NUMBER_OF_ADDR.getMessage()));
            }
        }
    }


    @Nested
    @DisplayName("PATCH /api/my/profile/locations/{memberAddrId} - 대표 주소 변경")
    class UpdateMainAddress {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("200 - 비대표 주소로 대표 주소를 변경하면 성공한다")
            void updateMainAddress_success() throws Exception {
                memberAddrRepository.save(MemberAddrFixture.createMainAddr(member));
                MemberAddr subAddr = memberAddrRepository.save(MemberAddrFixture.createSubAddr(member));
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(patch("/api/my/profile/locations/{memberAddrId}", subAddr.getId()))
                        .andExpect(status().isOk());
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("404 - 존재하지 않는 주소 ID로 변경하면 ADDRESS_NOT_FOUND 에러를 반환한다")
            void addressNotFound() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(patch("/api/my/profile/locations/{memberAddrId}", 999L))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value(MemberErrorCode.ADDRESS_NOT_FOUND.getCode()))
                        .andExpect(jsonPath("$.message").value(MemberErrorCode.ADDRESS_NOT_FOUND.getMessage()));
            }
        }
    }


    @Nested
    @DisplayName("DELETE /api/my/profile/locations/{memberAddrId} - 주소 삭제")
    class DeleteAddress {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("200 - 비대표 주소를 삭제하면 성공한다")
            void deleteSubAddress_success() throws Exception {
                memberAddrRepository.save(MemberAddrFixture.createMainAddr(member));
                MemberAddr subAddr = memberAddrRepository.save(MemberAddrFixture.createSubAddr(member));
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(delete("/api/my/profile/locations/{memberAddrId}", subAddr.getId()))
                        .andExpect(status().isOk());
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("400 - 대표 주소는 삭제할 수 없다")
            void cannotRemoveMainAddress() throws Exception {
                MemberAddr mainAddr = memberAddrRepository.save(MemberAddrFixture.createMainAddr(member));
                memberAddrRepository.save(MemberAddrFixture.createSubAddr(member));
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(delete("/api/my/profile/locations/{memberAddrId}", mainAddr.getId()))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(MemberErrorCode.CANNOT_REMOVE_MAIN_ADDR.getCode()))
                        .andExpect(jsonPath("$.message").value(MemberErrorCode.CANNOT_REMOVE_MAIN_ADDR.getMessage()));
            }

            @Test
            @DisplayName("400 - 주소가 1개뿐일 때 삭제하면 MEMBER_ADDRESS_MINIMUM_REQUIRED 에러를 반환한다")
            void minimumAddressRequired() throws Exception {
                // 비대표 주소 1개만 존재: isMain=false 이므로 첫 번째 체크(대표주소 여부)를 통과하고
                // 두 번째 체크(1개 이하)에서 예외 발생
                MemberAddr subAddr = memberAddrRepository.save(MemberAddrFixture.createSubAddr(member));
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(delete("/api/my/profile/locations/{memberAddrId}", subAddr.getId()))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(MemberErrorCode.MEMBER_ADDRESS_MINIMUM_REQUIRED.getCode()))
                        .andExpect(jsonPath("$.message").value(MemberErrorCode.MEMBER_ADDRESS_MINIMUM_REQUIRED.getMessage()));
            }
        }
    }


    @Nested
    @DisplayName("GET /api/my/location - 현재 위치 조회")
    class GetNowAddress {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("200 - 대표 주소가 있으면 현재 위치를 반환한다")
            void getNowAddress_success() throws Exception {
                memberAddrRepository.save(MemberAddrFixture.createMainAddr(member));
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/my/location"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.memberAddrId").isNumber())
                        .andExpect(jsonPath("$.data.addr3").value("역삼동"))
                        .andExpect(jsonPath("$.data.streetAddr").value("테헤란로 123"))
                        .andExpect(jsonPath("$.data.buildingName").value("ㅁㅁ빌딩"));
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("400 - 대표 주소가 없으면 MAIN_ADDRESS_NULL 에러를 반환한다")
            void noMainAddress_fail() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/my/location"))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(MemberErrorCode.MAIN_ADDRESS_NULL.getCode()))
                        .andExpect(jsonPath("$.message").value(MemberErrorCode.MAIN_ADDRESS_NULL.getMessage()));
            }
        }
    }

    // =====================================================
    // GET /api/my/profile/locations - 전체 주소 조회
    // =====================================================

    @Nested
    @DisplayName("GET /api/my/profile/locations - 전체 주소 조회")
    class GetAllAddress {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("200 - 전체 주소를 조회하면 대표 주소가 먼저 반환된다")
            void getAllAddress_mainFirst() throws Exception {
                memberAddrRepository.save(MemberAddrFixture.createSubAddr(member));
                memberAddrRepository.save(MemberAddrFixture.createMainAddr(member));
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/my/profile/locations"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data", hasSize(2)))
                        .andExpect(jsonPath("$.data[0].isMainAddr").value(true))
                        .andExpect(jsonPath("$.data[1].isMainAddr").value(false));
            }
        }
    }
}
