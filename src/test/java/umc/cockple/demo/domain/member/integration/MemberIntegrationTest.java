package umc.cockple.demo.domain.member.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import umc.cockple.demo.domain.chat.domain.ChatRoom;
import umc.cockple.demo.domain.chat.domain.ChatRoomMember;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomRepository;
import umc.cockple.demo.domain.contest.domain.Contest;
import umc.cockple.demo.domain.contest.enums.MedalType;
import umc.cockple.demo.domain.contest.repository.ContestRepository;
import umc.cockple.demo.domain.exercise.enums.ExerciseMemberShipStatus;
import umc.cockple.demo.domain.file.repository.ObjectStorageDeleteOutboxRepository;
import umc.cockple.demo.domain.file.service.FileService;
import umc.cockple.demo.domain.member.domain.*;
import umc.cockple.demo.domain.member.dto.CreateMemberAddrDTO;
import umc.cockple.demo.domain.member.dto.UpdateProfileRequestDTO;
import umc.cockple.demo.domain.member.enums.MemberStatus;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.repository.*;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.domain.PartyAddr;
import umc.cockple.demo.domain.party.enums.ParticipationType;
import umc.cockple.demo.domain.party.repository.PartyAddrRepository;
import umc.cockple.demo.domain.party.repository.PartyRepository;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Keyword;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.global.enums.Role;
import umc.cockple.demo.global.oauth2.service.KakaoOauthService;
import umc.cockple.demo.support.IntegrationTestBase;
import umc.cockple.demo.support.SecurityContextHelper;
import umc.cockple.demo.support.fixture.ChatFixture;
import umc.cockple.demo.support.fixture.MemberAddrFixture;
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MemberIntegrationTest extends IntegrationTestBase {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired MemberRepository memberRepository;
    @Autowired MemberAddrRepository memberAddrRepository;
    @Autowired MemberPartyRepository memberPartyRepository;
    @Autowired PartyRepository partyRepository;
    @Autowired PartyAddrRepository partyAddrRepository;

    @MockitoBean KakaoOauthService kakaoOauthService;
    @MockitoBean FileService fileService;

    @Autowired ContestRepository contestRepository;
    @Autowired MemberExerciseRepository memberExerciseRepository;
    @Autowired MemberKeywordRepository memberKeywordRepository;
    @Autowired ChatRoomRepository chatRoomRepository;
    @Autowired ChatRoomMemberRepository chatRoomMemberRepository;
    @Autowired ObjectStorageDeleteOutboxRepository objectStorageDeleteOutboxRepository;

    private Member member;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(MemberFixture.createMember("홍길동", Gender.MALE, Level.A, 1001L));
    }

    @AfterEach
    void tearDown() {
        objectStorageDeleteOutboxRepository.deleteAll(); // 프로필 이미지 교체 시 적재된 삭제 outbox 정리
        chatRoomRepository.deleteAll(); // cascade: ChatRoomMember 함께 삭제
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
                memberPartyRepository.save(MemberFixture.createMemberParty(party, member, Role.PARTY_MANAGER));

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
                memberPartyRepository.save(MemberFixture.createMemberParty(party, member, Role.PARTY_SUBMANAGER));

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
            @DisplayName("200 - 모든 필드가 정상 반환된다")
            void getProfile_모든_필드가_정상_반환된다() throws Exception {
                // given
                given(fileService.getUrlFromKey("profile/test-key.jpg"))
                        .willReturn("https://storage.googleapis.com/test-bucket/profile/test-key.jpg");

                Member freshMember = memberRepository.save(Member.builder()
                        .memberName("홍길동")
                        .nickname("홍길동")
                        .gender(Gender.MALE)
                        .birth(LocalDate.of(1990, 1, 1))
                        .level(Level.A)
                        .isActive(MemberStatus.ACTIVE)
                        .socialId(9001L)
                        .build());

                // ProfileImg: Member cascade를 통해 저장
                ProfileImg profileImg = ProfileImg.builder()
                        .member(freshMember)
                        .imgKey("profile/test-key.jpg")
                        .build();
                freshMember.updateProfileImg(profileImg);
                memberRepository.save(freshMember);

                // 금2, 은1, 동1
                for (int i = 0; i < 2; i++) {
                    contestRepository.save(Contest.builder()
                            .member(freshMember)
                            .contestName("금메달 대회")
                            .medalType(MedalType.GOLD)
                            .type(ParticipationType.SINGLE)
                            .level(Level.A)
                            .contentIsOpen(true)
                            .videoIsOpen(false)
                            .build());
                }
                contestRepository.save(Contest.builder()
                        .member(freshMember)
                        .contestName("은메달 대회")
                        .medalType(MedalType.SILVER)
                        .type(ParticipationType.SINGLE)
                        .level(Level.A)
                        .contentIsOpen(true)
                        .videoIsOpen(false)
                        .build());
                contestRepository.save(Contest.builder()
                        .member(freshMember)
                        .contestName("동메달 대회")
                        .medalType(MedalType.BRONZE)
                        .type(ParticipationType.SINGLE)
                        .level(Level.A)
                        .contentIsOpen(true)
                        .videoIsOpen(false)
                        .build());

                // 모임 2개
                memberPartyRepository.save(MemberFixture.createMemberParty(null, freshMember, Role.PARTY_MEMBER));
                memberPartyRepository.save(MemberFixture.createMemberParty(null, freshMember, Role.PARTY_MEMBER));

                SecurityContextHelper.setAuthentication(freshMember.getId(), freshMember.getNickname());

                // when & then
                mockMvc.perform(get("/api/profile/{memberId}", freshMember.getId()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.memberName").value("홍길동"))
                        .andExpect(jsonPath("$.data.birth").value("1990-01-01"))
                        .andExpect(jsonPath("$.data.gender").value("MALE"))
                        .andExpect(jsonPath("$.data.level").value("A"))
                        .andExpect(jsonPath("$.data.profileImgUrl").value("https://storage.googleapis.com/test-bucket/profile/test-key.jpg"))
                        .andExpect(jsonPath("$.data.myPartyCnt").value(2))
                        .andExpect(jsonPath("$.data.myGoldMedalCnt").value(2))
                        .andExpect(jsonPath("$.data.mySilverMedalCnt").value(1))
                        .andExpect(jsonPath("$.data.myBronzeMedalCnt").value(1));
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

            @Test
            @DisplayName("200 - 모든 필드가 정상 반환된다")
            void getMyProfile_모든_필드가_정상_반환된다() throws Exception {
                // given
                given(fileService.getUrlFromKey("profile/test-key.jpg"))
                        .willReturn("https://storage.googleapis.com/test-bucket/profile/test-key.jpg");

                Member freshMember = memberRepository.save(Member.builder()
                        .memberName("홍길동")
                        .nickname("홍길동")
                        .gender(Gender.MALE)
                        .birth(LocalDate.of(1990, 1, 1))
                        .level(Level.A)
                        .isActive(MemberStatus.ACTIVE)
                        .socialId(9002L)
                        .build());

                memberAddrRepository.save(MemberAddrFixture.createMainAddr(freshMember));

                // ProfileImg: Member cascade를 통해 저장
                ProfileImg profileImg = ProfileImg.builder()
                        .member(freshMember)
                        .imgKey("profile/test-key.jpg")
                        .build();
                freshMember.updateProfileImg(profileImg);
                memberRepository.save(freshMember);

                // 금2, 은1, 동1
                for (int i = 0; i < 2; i++) {
                    contestRepository.save(Contest.builder()
                            .member(freshMember)
                            .contestName("금메달 대회")
                            .medalType(MedalType.GOLD)
                            .type(ParticipationType.SINGLE)
                            .level(Level.A)
                            .contentIsOpen(true)
                            .videoIsOpen(false)
                            .build());
                }
                contestRepository.save(Contest.builder()
                        .member(freshMember)
                        .contestName("은메달 대회")
                        .medalType(MedalType.SILVER)
                        .type(ParticipationType.SINGLE)
                        .level(Level.A)
                        .contentIsOpen(true)
                        .videoIsOpen(false)
                        .build());
                contestRepository.save(Contest.builder()
                        .member(freshMember)
                        .contestName("동메달 대회")
                        .medalType(MedalType.BRONZE)
                        .type(ParticipationType.SINGLE)
                        .level(Level.A)
                        .contentIsOpen(true)
                        .videoIsOpen(false)
                        .build());

                // 모임 2개, 운동 2개, 키워드 2개
                memberPartyRepository.save(MemberFixture.createMemberParty(null, freshMember, Role.PARTY_MEMBER));
                memberPartyRepository.save(MemberFixture.createMemberParty(null, freshMember, Role.PARTY_MEMBER));

                memberExerciseRepository.save(MemberExercise.builder()
                        .member(freshMember)
                        .exerciseMemberShipStatus(ExerciseMemberShipStatus.PARTY_MEMBER)
                        .build());
                memberExerciseRepository.save(MemberExercise.builder()
                        .member(freshMember)
                        .exerciseMemberShipStatus(ExerciseMemberShipStatus.PARTY_MEMBER)
                        .build());

                memberKeywordRepository.save(MemberKeyword.builder()
                        .member(freshMember)
                        .keyword(Keyword.FRIENDSHIP)
                        .build());
                memberKeywordRepository.save(MemberKeyword.builder()
                        .member(freshMember)
                        .keyword(Keyword.FREE)
                        .build());

                SecurityContextHelper.setAuthentication(freshMember.getId(), freshMember.getNickname());

                // when & then
                mockMvc.perform(get("/api/my/profile"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.memberName").value("홍길동"))
                        .andExpect(jsonPath("$.data.birth").value("1990-01-01"))
                        .andExpect(jsonPath("$.data.gender").value("MALE"))
                        .andExpect(jsonPath("$.data.level").value("A"))
                        .andExpect(jsonPath("$.data.keywords", hasSize(2)))
                        .andExpect(jsonPath("$.data.addr3").value("역삼동"))
                        .andExpect(jsonPath("$.data.streetAddr").value("테헤란로 123"))
                        .andExpect(jsonPath("$.data.buildingName").value("ㅁㅁ빌딩"))
                        .andExpect(jsonPath("$.data.latitude").value(37.5))
                        .andExpect(jsonPath("$.data.longitude").value(127.0))
                        .andExpect(jsonPath("$.data.profileImgUrl").value("https://storage.googleapis.com/test-bucket/profile/test-key.jpg"))
                        .andExpect(jsonPath("$.data.myPartyCnt").value(2))
                        .andExpect(jsonPath("$.data.myExerciseCnt").value(2))
                        .andExpect(jsonPath("$.data.myGoldMedalCnt").value(2))
                        .andExpect(jsonPath("$.data.mySilverMedalCnt").value(1))
                        .andExpect(jsonPath("$.data.myBronzeMedalCnt").value(1));
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
    @DisplayName("PATCH /api/my/profile - 프로필 수정")
    class UpdateProfile {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("200 - 모든 필드가 정상 업데이트된다")
            void 모든_필드가_정상_업데이트된다() throws Exception {
                // given - 기존 키워드 등록
                memberKeywordRepository.save(MemberKeyword.builder()
                        .member(member).keyword(Keyword.FREE).build());

                UpdateProfileRequestDTO request = new UpdateProfileRequestDTO(
                        "김길동", LocalDate.of(1995, 6, 15), Level.B,
                        List.of(Keyword.FRIENDSHIP, Keyword.MANAGER_MATCH), "profile/new-key.jpg");

                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                // when
                mockMvc.perform(patch("/api/my/profile")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk());

                // then - DB에서 모든 필드 검증
                Member updated = memberRepository.findMemberWithProfileById(member.getId()).orElseThrow();
                assertThat(updated.getMemberName()).isEqualTo("김길동");
                assertThat(updated.getBirth()).isEqualTo(LocalDate.of(1995, 6, 15));
                assertThat(updated.getLevel()).isEqualTo(Level.B);
                assertThat(updated.getProfileImg()).isNotNull();
                assertThat(updated.getProfileImg().getImgKey()).isEqualTo("profile/new-key.jpg");

                List<MemberKeyword> keywords = memberKeywordRepository.findAllByMemberId(member.getId());
                assertThat(keywords).hasSize(2);
                assertThat(keywords.stream().map(MemberKeyword::getKeyword).toList())
                        .containsExactlyInAnyOrder(Keyword.FRIENDSHIP, Keyword.MANAGER_MATCH);
            }

            @Test
            @DisplayName("200 - imgKey가 없으면 이미지 없이 프로필이 업데이트된다")
            void imgKey가_없으면_이미지_없이_프로필이_업데이트된다() throws Exception {
                // given
                UpdateProfileRequestDTO request = new UpdateProfileRequestDTO(
                        "김길동", LocalDate.of(1990, 1, 1), Level.A,
                        List.of(Keyword.FRIENDSHIP), null);

                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                // when
                mockMvc.perform(patch("/api/my/profile")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk());

                // then
                Member updated = memberRepository.findMemberWithProfileById(member.getId()).orElseThrow();
                assertThat(updated.getMemberName()).isEqualTo("김길동");
                assertThat(updated.getProfileImg()).isNull();
            }

            @Test
            @DisplayName("200 - 기존 이미지가 있고 imgKey가 다르면 이미지가 변경된다")
            void 기존_이미지가_있고_imgKey가_다르면_이미지가_변경된다() throws Exception {
                // given - 기존 프로필 이미지 설정
                ProfileImg existingImg = ProfileImg.builder()
                        .member(member).imgKey("profile/old-key.jpg").build();
                member.updateProfileImg(existingImg);
                memberRepository.save(member);

                UpdateProfileRequestDTO request = new UpdateProfileRequestDTO(
                        "김길동", LocalDate.of(1990, 1, 1), Level.A,
                        List.of(Keyword.FRIENDSHIP), "profile/new-key.jpg");

                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                // when
                mockMvc.perform(patch("/api/my/profile")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk());

                // then
                Member updated = memberRepository.findMemberWithProfileById(member.getId()).orElseThrow();
                assertThat(updated.getProfileImg().getImgKey()).isEqualTo("profile/new-key.jpg");
            }

            @Test
            @DisplayName("200 - DIRECT 채팅방 상대방의 displayName이 업데이트된다")
            void DIRECT_채팅방_상대방의_displayName이_업데이트된다() throws Exception {
                // given
                Member counterPart = memberRepository.save(
                        MemberFixture.createMember("상대방", Gender.FEMALE, Level.B, 2001L));

                ChatRoom directRoom = ChatRoom.createDirectChatRoom();
                directRoom.addChatRoomMember(
                        ChatFixture.createJoinedMember(directRoom, member, "홍길동"));
                directRoom.addChatRoomMember(
                        ChatFixture.createJoinedMember(directRoom, counterPart, "홍길동"));
                chatRoomRepository.save(directRoom);

                UpdateProfileRequestDTO request = new UpdateProfileRequestDTO(
                        "김길동", LocalDate.of(1990, 1, 1), Level.A,
                        List.of(Keyword.FRIENDSHIP), null);

                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                // when
                mockMvc.perform(patch("/api/my/profile")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk());

                // then - 상대방의 ChatRoomMember displayName이 업데이트되었는지 검증
                ChatRoomMember updatedCounterPart = chatRoomMemberRepository
                        .findByChatRoomIdAndMemberId(directRoom.getId(), counterPart.getId())
                        .orElseThrow();
                assertThat(updatedCounterPart.getDisplayName()).isEqualTo("김길동");
            }

            @Test
            @DisplayName("200 - PARTY 채팅방의 displayName은 변경되지 않는다")
            void PARTY_채팅방의_displayName은_변경되지_않는다() throws Exception {
                // given
                PartyAddr addr = partyAddrRepository.save(PartyFixture.createPartyAddr("서울특별시", "강남구"));
                Party party = partyRepository.save(PartyFixture.createParty("테스트 모임", member.getId(), addr));

                ChatRoom partyRoom = ChatRoom.createPartyChatRoom(party);
                partyRoom.addChatRoomMember(
                        ChatFixture.createJoinedMember(partyRoom, member, "홍길동"));
                chatRoomRepository.save(partyRoom);

                UpdateProfileRequestDTO request = new UpdateProfileRequestDTO(
                        "김길동", LocalDate.of(1990, 1, 1), Level.A,
                        List.of(Keyword.FRIENDSHIP), null);

                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                // when
                mockMvc.perform(patch("/api/my/profile")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk());

                // then - PARTY 채팅방의 ChatRoomMember displayName은 변경되지 않아야 한다
                ChatRoomMember partyChatMember = chatRoomMemberRepository
                        .findByChatRoomIdAndMemberId(partyRoom.getId(), member.getId())
                        .orElseThrow();
                assertThat(partyChatMember.getDisplayName()).isEqualTo("홍길동");
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("404 - 존재하지 않는 회원이면 MEMBER_NOT_FOUND 에러를 반환한다")
            void 존재하지_않는_회원이면_MEMBER_NOT_FOUND_에러를_반환한다() throws Exception {
                UpdateProfileRequestDTO request = new UpdateProfileRequestDTO(
                        "김길동", LocalDate.of(1990, 1, 1), Level.A,
                        List.of(Keyword.FRIENDSHIP), null);

                SecurityContextHelper.setAuthentication(999L, "없는회원");

                mockMvc.perform(patch("/api/my/profile")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value(MemberErrorCode.MEMBER_NOT_FOUND.getCode()))
                        .andExpect(jsonPath("$.message").value(MemberErrorCode.MEMBER_NOT_FOUND.getMessage()));
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

            @Test
            @DisplayName("200 - 주소의 모든 필드가 정상 반환된다")
            void getAllAddress_모든_필드가_정상_반환된다() throws Exception {
                // given
                MemberAddr mainAddr = memberAddrRepository.save(MemberAddrFixture.createMainAddr(member));
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                // when & then
                mockMvc.perform(get("/api/my/profile/locations"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data", hasSize(1)))
                        .andExpect(jsonPath("$.data[0].addrId").value(mainAddr.getId()))
                        .andExpect(jsonPath("$.data[0].addr1").value("서울특별시"))
                        .andExpect(jsonPath("$.data[0].addr2").value("강남구"))
                        .andExpect(jsonPath("$.data[0].addr3").value("역삼동"))
                        .andExpect(jsonPath("$.data[0].streetAddr").value("테헤란로 123"))
                        .andExpect(jsonPath("$.data[0].buildingName").value("ㅁㅁ빌딩"))
                        .andExpect(jsonPath("$.data[0].latitude").value(37.5))
                        .andExpect(jsonPath("$.data[0].longitude").value(127.0))
                        .andExpect(jsonPath("$.data[0].isMainAddr").value(true));
            }
        }
    }
}
