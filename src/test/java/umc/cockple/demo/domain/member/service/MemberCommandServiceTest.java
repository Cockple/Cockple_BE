package umc.cockple.demo.domain.member.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.file.service.FileService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberAddr;
import umc.cockple.demo.domain.member.domain.MemberParty;
import umc.cockple.demo.domain.member.domain.ProfileImg;
import umc.cockple.demo.domain.member.dto.MemberDetailInfoRequestDTO;
import umc.cockple.demo.domain.member.dto.UpdateProfileRequestDTO;
import umc.cockple.demo.domain.member.enums.MemberPartyStatus;
import umc.cockple.demo.domain.member.enums.MemberStatus;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.exception.MemberException;
import umc.cockple.demo.domain.member.repository.*;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Keyword;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.global.enums.Role;
import umc.cockple.demo.global.oauth2.service.KakaoOauthService;
import umc.cockple.demo.support.fixture.MemberAddrFixture;
import umc.cockple.demo.support.fixture.MemberFixture;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import static umc.cockple.demo.domain.member.dto.CreateMemberAddrDTO.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberCommandService")
class MemberCommandServiceTest {

    @InjectMocks
    private MemberCommandService memberCommandService;

    @Mock private MemberRepository memberRepository;
    @Mock private MemberExerciseRepository memberExerciseRepository;
    @Mock private MemberPartyRepository memberPartyRepository;
    @Mock private MemberKeywordRepository memberKeywordRepository;
    @Mock private MemberAddrRepository memberAddrRepository;
    @Mock private ChatRoomMemberRepository chatRoomMemberRepository;
    @Mock private FileService fileService;
    @Mock private KakaoOauthService kakaoOauthService;

    private Member normalMember;

    @BeforeEach
    void setUp() {
        normalMember = MemberFixture.createMember("와나", Gender.MALE, Level.C, 9001L);
        ReflectionTestUtils.setField(normalMember, "id", 1L);
    }

    @Nested
    @DisplayName("registerMemberDetailInfo")
    class RegisterMemberDetailInfo {

        private MemberDetailInfoRequestDTO requestWithImg;
        private MemberDetailInfoRequestDTO requestWithoutImg;

        @BeforeEach
        void setUp() {
            requestWithImg = MemberDetailInfoRequestDTO.builder()
                    .memberName("강와나")
                    .gender(Gender.MALE)
                    .birth(LocalDate.of(2002, 4, 2))
                    .level(Level.A)
                    .imgKey("profile/test-key.jpg")
                    .keywords(List.of(Keyword.FRIENDSHIP, Keyword.FREE))
                    .build();

            requestWithoutImg = MemberDetailInfoRequestDTO.builder()
                    .memberName("강와나")
                    .gender(Gender.MALE)
                    .birth(LocalDate.of(2002, 4, 2))
                    .level(Level.A)
                    .imgKey(null)
                    .keywords(List.of(Keyword.FRIENDSHIP))
                    .build();
        }

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("imgKey가_있으면_ProfileImg와_함께_회원정보가_업데이트된다")
            void imgKey가_있으면_ProfileImg와_함께_회원정보가_업데이트된다() {
                // given
                given(memberRepository.findById(normalMember.getId()))
                        .willReturn(Optional.of(normalMember));

                // when
                memberCommandService.memberDetailInfo(normalMember.getId(), requestWithImg);

                // then
                then(memberKeywordRepository).should().saveAll(any());

                assertThat(normalMember.getMemberName()).isEqualTo("강와나");
                assertThat(normalMember.getGender()).isEqualTo(Gender.MALE);
                assertThat(normalMember.getBirth()).isEqualTo(LocalDate.of(2002, 4, 2));
                assertThat(normalMember.getLevel()).isEqualTo(Level.A);
                assertThat(normalMember.getProfileImg()).isNotNull();
                assertThat(normalMember.getProfileImg().getImgKey()).isEqualTo("profile/test-key.jpg");
            }

            @Test
            @DisplayName("imgKey가_없으면_ProfileImg_없이_회원정보가_업데이트된다")
            void imgKey가_없으면_ProfileImg_없이_회원정보가_업데이트된다() {
                // given
                given(memberRepository.findById(normalMember.getId()))
                        .willReturn(Optional.of(normalMember));

                // when
                memberCommandService.memberDetailInfo(normalMember.getId(), requestWithoutImg);

                // then
                then(memberKeywordRepository).should().saveAll(any());

                assertThat(normalMember.getMemberName()).isEqualTo("강와나");
                assertThat(normalMember.getLevel()).isEqualTo(Level.A);
                assertThat(normalMember.getProfileImg()).isNull();
            }

            @Test
            @DisplayName("keywords가_정상적으로_저장된다")
            void keywords가_정상적으로_저장된다() {
                // given
                given(memberRepository.findById(normalMember.getId()))
                        .willReturn(Optional.of(normalMember));

                // when
                memberCommandService.memberDetailInfo(normalMember.getId(), requestWithImg);

                // then
                // saveAll 호출 시 keywords 개수가 request와 동일한지 검증
                assertThat(normalMember.getKeywords()).hasSize(requestWithImg.keywords().size());
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("존재하지_않는_회원이면_MEMBER_NOT_FOUND_예외를_던진다")
            void 존재하지_않는_회원이면_MEMBER_NOT_FOUND_예외를_던진다() {
                // given
                given(memberRepository.findById(999L))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() ->
                        memberCommandService.memberDetailInfo(999L, requestWithImg))
                        .isInstanceOf(MemberException.class)
                        .hasFieldOrPropertyWithValue("code", MemberErrorCode.MEMBER_NOT_FOUND);
            }
        }
    }

    @Nested
    @DisplayName("updateProfile")
    class UpdateProfile {

        private UpdateProfileRequestDTO requestWithImg;
        private UpdateProfileRequestDTO requestWithoutImg;

        @BeforeEach
        void setUp() {
            requestWithImg = new UpdateProfileRequestDTO(
                    "강와나", LocalDate.of(2002, 4, 2), Level.A,
                    List.of(Keyword.FRIENDSHIP, Keyword.FREE), "profile/new-key.jpg");

            requestWithoutImg = new UpdateProfileRequestDTO(
                    "강와나", LocalDate.of(2002, 4, 2), Level.A,
                    List.of(Keyword.FRIENDSHIP), null);
        }

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("imgKey가_없으면_이미지_없이_프로필이_업데이트된다")
            void imgKey가_없으면_이미지_없이_프로필이_업데이트된다() {
                // given
                given(memberRepository.findMemberWithProfileById(normalMember.getId()))
                        .willReturn(Optional.of(normalMember));
                given(chatRoomMemberRepository.findAllByMemberId(normalMember.getId()))
                        .willReturn(List.of());

                // when
                memberCommandService.updateProfile(requestWithoutImg, normalMember.getId());

                // then
                then(memberKeywordRepository).should().deleteAllByMember(normalMember);
                then(memberKeywordRepository).should().saveAll(any());
                assertThat(normalMember.getMemberName()).isEqualTo("강와나");
                assertThat(normalMember.getLevel()).isEqualTo(Level.A);
            }

            @Test
            @DisplayName("기존_이미지가_없고_imgKey가_있으면_새_ProfileImg를_생성해_업데이트된다")
            void 기존_이미지가_없고_imgKey가_있으면_새_ProfileImg를_생성해_업데이트된다() {
                // given
                given(memberRepository.findMemberWithProfileById(normalMember.getId()))
                        .willReturn(Optional.of(normalMember));
                given(chatRoomMemberRepository.findAllByMemberId(normalMember.getId()))
                        .willReturn(List.of());

                // when
                memberCommandService.updateProfile(requestWithImg, normalMember.getId());

                // then
                assertThat(normalMember.getProfileImg()).isNotNull();
                assertThat(normalMember.getProfileImg().getImgKey()).isEqualTo("profile/new-key.jpg");
            }

            @Test
            @DisplayName("기존_이미지가_있고_imgKey가_다르면_기존_이미지를_삭제하고_업데이트된다")
            void 기존_이미지가_있고_imgKey가_다르면_기존_이미지를_삭제하고_업데이트된다() {
                // given
                ProfileImg existingProfile = ProfileImg.builder()
                        .member(normalMember)
                        .imgKey("profile/old-key.jpg")
                        .build();
                ReflectionTestUtils.setField(normalMember, "profileImg", existingProfile);

                given(memberRepository.findMemberWithProfileById(normalMember.getId()))
                        .willReturn(Optional.of(normalMember));
                given(chatRoomMemberRepository.findAllByMemberId(normalMember.getId()))
                        .willReturn(List.of());

                // when
                memberCommandService.updateProfile(requestWithImg, normalMember.getId());

                // then
                then(fileService).should().delete("profile/old-key.jpg");
            }

            @Test
            @DisplayName("기존_이미지가_있고_imgKey가_같으면_삭제_없이_업데이트된다")
            void 기존_이미지가_있고_imgKey가_같으면_삭제_없이_업데이트된다() {
                // given
                UpdateProfileRequestDTO sameImgRequest = new UpdateProfileRequestDTO(
                        "강와나", LocalDate.of(2002, 4, 2), Level.A,
                        List.of(Keyword.FRIENDSHIP), "profile/same-key.jpg");

                ProfileImg existingProfile = ProfileImg.builder()
                        .member(normalMember)
                        .imgKey("profile/same-key.jpg")
                        .build();
                ReflectionTestUtils.setField(normalMember, "profileImg", existingProfile);

                given(memberRepository.findMemberWithProfileById(normalMember.getId()))
                        .willReturn(Optional.of(normalMember));
                given(chatRoomMemberRepository.findAllByMemberId(normalMember.getId()))
                        .willReturn(List.of());

                // when
                memberCommandService.updateProfile(sameImgRequest, normalMember.getId());

                // then
                then(fileService).should(never()).delete(any());
            }

            @Test
            @DisplayName("기존_키워드를_삭제하고_새_키워드를_저장한다")
            void 기존_키워드를_삭제하고_새_키워드를_저장한다() {
                // given
                given(memberRepository.findMemberWithProfileById(normalMember.getId()))
                        .willReturn(Optional.of(normalMember));
                given(chatRoomMemberRepository.findAllByMemberId(normalMember.getId()))
                        .willReturn(List.of());

                // when
                memberCommandService.updateProfile(requestWithImg, normalMember.getId());

                // then
                then(memberKeywordRepository).should().deleteAllByMember(normalMember);
                then(memberKeywordRepository).should().saveAll(any());
                assertThat(normalMember.getKeywords()).hasSize(requestWithImg.keywords().size());
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("존재하지_않는_회원이면_MEMBER_NOT_FOUND_예외를_던진다")
            void 존재하지_않는_회원이면_MEMBER_NOT_FOUND_예외를_던진다() {
                // given
                given(memberRepository.findMemberWithProfileById(999L))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> memberCommandService.updateProfile(requestWithImg, 999L))
                        .isInstanceOf(MemberException.class)
                        .hasFieldOrPropertyWithValue("code", MemberErrorCode.MEMBER_NOT_FOUND);
            }
        }
    }

    @Nested
    @DisplayName("addMemberNewAddr")
    class AddMemberNewAddr {

        private CreateMemberAddrRequestDTO requestDto;

        @BeforeEach
        void setUp() {
            requestDto = new CreateMemberAddrRequestDTO(
                    "서울특별시", "강남구", "역삼동",
                    "테헤란로 123", "테스트빌딩",
                    37.5, 127.0);
        }

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("정상적으로_주소를_등록하면_새_주소_ID를_반환한다")
            void 정상적으로_주소를_등록하면_새_주소_ID를_반환한다() {
                // given
                MemberAddr savedAddr = MemberAddrFixture.createSeoulAddr(normalMember, true);
                ReflectionTestUtils.setField(savedAddr, "id", 10L);

                given(memberRepository.findById(normalMember.getId()))
                        .willReturn(Optional.of(normalMember));
                given(memberAddrRepository.save(any())).willReturn(savedAddr);

                // when
                CreateMemberAddrResponseDTO response =
                        memberCommandService.addMemberNewAddr(requestDto, normalMember.getId());

                // then
                assertThat(response.memberAddrId()).isEqualTo(10L);
            }

            @Test
            @DisplayName("기존_대표주소가_있으면_대표주소가_false로_해제된다")
            void 기존_대표주소가_있으면_대표주소가_false로_해제된다() {
                // given
                MemberAddr existingMainAddr = MemberAddrFixture.createBusanAddr(normalMember, true);
                normalMember.getAddresses().add(existingMainAddr);

                MemberAddr newAddr = MemberAddrFixture.createSeoulAddr(normalMember, true);
                ReflectionTestUtils.setField(newAddr, "id", 11L);

                given(memberRepository.findById(normalMember.getId()))
                        .willReturn(Optional.of(normalMember));
                given(memberAddrRepository.save(any())).willReturn(newAddr);

                // when
                memberCommandService.addMemberNewAddr(requestDto, normalMember.getId());

                // then
                assertThat(existingMainAddr.getIsMain()).isFalse();
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("존재하지_않는_회원이면_MEMBER_NOT_FOUND_예외를_던진다")
            void 존재하지_않는_회원이면_MEMBER_NOT_FOUND_예외를_던진다() {
                // given
                given(memberRepository.findById(999L)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> memberCommandService.addMemberNewAddr(requestDto, 999L))
                        .isInstanceOf(MemberException.class)
                        .hasFieldOrPropertyWithValue("code", MemberErrorCode.MEMBER_NOT_FOUND);
            }

            @Test
            @DisplayName("중복_주소이면_DUPLICATE_ADDRESS_예외를_던진다")
            void 중복_주소이면_DUPLICATE_ADDRESS_예외를_던진다() {
                // given
                // requestDto(서울/강남/역삼/테헤란로 123/테스트빌딩/37.5/127.0)와 동일한 주소를 미리 등록
                MemberAddr existingAddr = MemberAddrFixture.createSeoulAddr(normalMember, true);
                normalMember.getAddresses().add(existingAddr);

                given(memberRepository.findById(normalMember.getId()))
                        .willReturn(Optional.of(normalMember));

                // when & then
                assertThatThrownBy(() ->
                        memberCommandService.addMemberNewAddr(requestDto, normalMember.getId()))
                        .isInstanceOf(MemberException.class)
                        .hasFieldOrPropertyWithValue("code", MemberErrorCode.DUPLICATE_ADDRESS);
            }

            @Test
            @DisplayName("주소가_5개_이상이면_OVER_NUMBER_OF_ADDR_예외를_던진다")
            void 주소가_5개_이상이면_OVER_NUMBER_OF_ADDR_예외를_던진다() {
                // given
                for (int i = 0; i < 5; i++) {
                    normalMember.getAddresses().add(
                            MemberAddrFixture.createAddrWithIndex(normalMember, i, i == 0));
                }

                given(memberRepository.findById(normalMember.getId()))
                        .willReturn(Optional.of(normalMember));

                // when & then
                assertThatThrownBy(() ->
                        memberCommandService.addMemberNewAddr(requestDto, normalMember.getId()))
                        .isInstanceOf(MemberException.class)
                        .hasFieldOrPropertyWithValue("code", MemberErrorCode.OVER_NUMBER_OF_ADDR);
            }
        }
    }

    @Nested
    @DisplayName("updateMainAddr")
    class UpdateMainAddr {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("기존_대표주소가_해제되고_새_대표주소가_설정된다")
            void 기존_대표주소가_해제되고_새_대표주소가_설정된다() {
                // given
                MemberAddr oldMainAddr = MemberAddrFixture.createBusanAddr(normalMember, true);
                normalMember.getAddresses().add(oldMainAddr);

                MemberAddr newMainAddr = MemberAddrFixture.createSeoulAddr(normalMember, false);
                ReflectionTestUtils.setField(newMainAddr, "id", 20L);

                given(memberRepository.findById(normalMember.getId()))
                        .willReturn(Optional.of(normalMember));
                given(memberAddrRepository.findById(20L))
                        .willReturn(Optional.of(newMainAddr));

                // when
                memberCommandService.updateMainAddr(normalMember.getId(), 20L);

                // then
                assertThat(oldMainAddr.getIsMain()).isFalse();
                assertThat(newMainAddr.getIsMain()).isTrue();
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("존재하지_않는_주소이면_ADDRESS_NOT_FOUND_예외를_던진다")
            void 존재하지_않는_주소이면_ADDRESS_NOT_FOUND_예외를_던진다() {
                // given
                given(memberRepository.findById(normalMember.getId()))
                        .willReturn(Optional.of(normalMember));
                given(memberAddrRepository.findById(999L)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() ->
                        memberCommandService.updateMainAddr(normalMember.getId(), 999L))
                        .isInstanceOf(MemberException.class)
                        .hasFieldOrPropertyWithValue("code", MemberErrorCode.ADDRESS_NOT_FOUND);
            }
        }
    }

    // =====================================================================
    // deleteMemberAddr
    // =====================================================================

    @Nested
    @DisplayName("deleteMemberAddr")
    class DeleteMemberAddr {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("대표주소가_아닌_주소를_삭제하면_정상_삭제된다")
            void 대표주소가_아닌_주소를_삭제하면_정상_삭제된다() {
                // given
                MemberAddr mainAddr = MemberAddrFixture.createMainAddr(normalMember);
                MemberAddr subAddr = MemberAddrFixture.createSubAddr(normalMember);
                ReflectionTestUtils.setField(subAddr, "id", 30L);
                normalMember.getAddresses().add(mainAddr);
                normalMember.getAddresses().add(subAddr);

                given(memberRepository.findById(normalMember.getId()))
                        .willReturn(Optional.of(normalMember));
                given(memberAddrRepository.findById(30L)).willReturn(Optional.of(subAddr));

                // when
                memberCommandService.deleteMemberAddr(normalMember.getId(), 30L);

                // then
                then(memberAddrRepository).should().deleteById(30L);
                assertThat(normalMember.getAddresses()).doesNotContain(subAddr);
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {


            @Test
            @DisplayName("존재하지_않는_주소이면_ADDRESS_NOT_FOUND_예외를_던진다")
            void 존재하지_않는_주소이면_ADDRESS_NOT_FOUND_예외를_던진다() {
                // given
                given(memberRepository.findById(normalMember.getId()))
                        .willReturn(Optional.of(normalMember));
                given(memberAddrRepository.findById(999L)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() ->
                        memberCommandService.deleteMemberAddr(normalMember.getId(), 999L))
                        .isInstanceOf(MemberException.class)
                        .hasFieldOrPropertyWithValue("code", MemberErrorCode.ADDRESS_NOT_FOUND);
            }

            @Test
            @DisplayName("대표주소를_삭제하면_CANNOT_REMOVE_MAIN_ADDR_예외를_던진다")
            void 대표주소를_삭제하면_CANNOT_REMOVE_MAIN_ADDR_예외를_던진다() {
                // given
                MemberAddr mainAddr = MemberAddrFixture.createMainAddr(normalMember);
                ReflectionTestUtils.setField(mainAddr, "id", 31L);
                normalMember.getAddresses().add(mainAddr);

                given(memberRepository.findById(normalMember.getId()))
                        .willReturn(Optional.of(normalMember));
                given(memberAddrRepository.findById(31L)).willReturn(Optional.of(mainAddr));

                // when & then
                assertThatThrownBy(() ->
                        memberCommandService.deleteMemberAddr(normalMember.getId(), 31L))
                        .isInstanceOf(MemberException.class)
                        .hasFieldOrPropertyWithValue("code", MemberErrorCode.CANNOT_REMOVE_MAIN_ADDR);
            }

            @Test
            @DisplayName("주소가_1개이면_MEMBER_ADDRESS_MINIMUM_REQUIRED_예외를_던진다")
            void 주소가_1개이면_MEMBER_ADDRESS_MINIMUM_REQUIRED_예외를_던진다() {
                // given
                MemberAddr onlySubAddr = MemberAddrFixture.createSubAddr(normalMember);
                ReflectionTestUtils.setField(onlySubAddr, "id", 32L);
                normalMember.getAddresses().add(onlySubAddr);

                given(memberRepository.findById(normalMember.getId()))
                        .willReturn(Optional.of(normalMember));
                given(memberAddrRepository.findById(32L)).willReturn(Optional.of(onlySubAddr));

                // when & then
                assertThatThrownBy(() ->
                        memberCommandService.deleteMemberAddr(normalMember.getId(), 32L))
                        .isInstanceOf(MemberException.class)
                        .hasFieldOrPropertyWithValue("code", MemberErrorCode.MEMBER_ADDRESS_MINIMUM_REQUIRED);
            }
        }
    }

    @Nested
    @DisplayName("withdrawMember")
    class WithdrawMember {

        @Test
        @DisplayName("과거_운동은_삭제되지_않고_미래_운동만_삭제한다")
        void 과거_운동은_삭제되지_않고_미래_운동만_삭제한다() {
            // given
            given(memberRepository.findById(normalMember.getId())).willReturn(Optional.of(normalMember));

            // when
            memberCommandService.withdrawMember(normalMember.getId());

            // then
            then(memberExerciseRepository).should()
                    .deleteFutureExercisesByMember(eq(normalMember), any(), any());
            then(memberExerciseRepository).should(never())
                    .deleteAll();
        }

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("미래_운동만_삭제되고_모임과_키워드도_삭제된다")
            void 미래_운동만_삭제되고_모임과_키워드도_삭제된다() {
                // given
                given(memberRepository.findById(normalMember.getId()))
                        .willReturn(Optional.of(normalMember));

                // when
                memberCommandService.withdrawMember(normalMember.getId());

                // then
                then(memberExerciseRepository).should()
                        .deleteFutureExercisesByMember(eq(normalMember), any(), any());
                then(memberExerciseRepository).should(never()).deleteAll();
                then(memberPartyRepository).should().deleteAllByMember(normalMember);
                then(memberKeywordRepository).should().deleteAllByMember(normalMember);
            }

            @Test
            @DisplayName("탈퇴_후_회원_상태가_INACTIVE가_되고_refreshToken이_null이_된다")
            void 탈퇴_후_회원_상태가_INACTIVE가_되고_refreshToken이_null이_된다() {
                // given
                normalMember.setRefreshToken("existing-refresh-token");
                given(memberRepository.findById(normalMember.getId()))
                        .willReturn(Optional.of(normalMember));

                // when
                memberCommandService.withdrawMember(normalMember.getId());

                // then
                assertThat(normalMember.getIsActive()).isEqualTo(MemberStatus.INACTIVE);
                assertThat(normalMember.getRefreshToken()).isNull();
            }

            @Test
            @DisplayName("카카오_연결_끊기가_호출된다")
            void 카카오_연결_끊기가_호출된다() {
                // given
                given(memberRepository.findById(normalMember.getId()))
                        .willReturn(Optional.of(normalMember));

                // when
                memberCommandService.withdrawMember(normalMember.getId());

                // then
                then(kakaoOauthService).should().unlinkAccess(normalMember);
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("존재하지_않는_회원이면_MEMBER_NOT_FOUND_예외를_던진다")
            void 존재하지_않는_회원이면_MEMBER_NOT_FOUND_예외를_던진다() {
                // given
                given(memberRepository.findById(999L))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> memberCommandService.withdrawMember(999L))
                        .isInstanceOf(MemberException.class)
                        .hasFieldOrPropertyWithValue("code", MemberErrorCode.MEMBER_NOT_FOUND);
            }

            @Test
            @DisplayName("이미_탈퇴한_회원이면_ALREADY_WITHDRAW_예외를_던진다")
            void 이미_탈퇴한_회원이면_ALREADY_WITHDRAW_예외를_던진다() {
                // given
                Member withdrawnMember = MemberFixture.createWithdrawnMember("탈퇴회원", "탈퇴닉", 9002L);
                ReflectionTestUtils.setField(withdrawnMember, "id", 2L);

                given(memberRepository.findById(withdrawnMember.getId()))
                        .willReturn(Optional.of(withdrawnMember));

                // when & then
                assertThatThrownBy(() -> memberCommandService.withdrawMember(withdrawnMember.getId()))
                        .isInstanceOf(MemberException.class)
                        .hasFieldOrPropertyWithValue("code", MemberErrorCode.ALREADY_WITHDRAW);
            }

            @Test
            @DisplayName("활성_모임의_모임장이면_MANAGER_CANNOT_LEAVE_예외를_던진다")
            void 활성_모임의_모임장이면_MANAGER_CANNOT_LEAVE_예외를_던진다() {
                // given
                MemberParty leaderParty = MemberParty.builder()
                        .role(Role.party_MANAGER)
                        .status(MemberPartyStatus.ACTIVE)
                        .joinedAt(LocalDateTime.now())
                        .build();
                normalMember.getMemberParties().add(leaderParty);

                given(memberRepository.findById(normalMember.getId()))
                        .willReturn(Optional.of(normalMember));

                // when & then
                assertThatThrownBy(() -> memberCommandService.withdrawMember(normalMember.getId()))
                        .isInstanceOf(MemberException.class)
                        .hasFieldOrPropertyWithValue("code", MemberErrorCode.MANAGER_CANNOT_LEAVE);
            }

            @Test
            @DisplayName("활성_모임의_부모임장이면_SUBMANAGER_CANNOT_LEAVE_예외를_던진다")
            void 활성_모임의_부모임장이면_SUBMANAGER_CANNOT_LEAVE_예외를_던진다() {
                // given
                MemberParty subManagerParty = MemberParty.builder()
                        .role(Role.party_SUBMANAGER)
                        .status(MemberPartyStatus.ACTIVE)
                        .joinedAt(LocalDateTime.now())
                        .build();
                normalMember.getMemberParties().add(subManagerParty);

                given(memberRepository.findById(normalMember.getId()))
                        .willReturn(Optional.of(normalMember));

                // when & then
                assertThatThrownBy(() -> memberCommandService.withdrawMember(normalMember.getId()))
                        .isInstanceOf(MemberException.class)
                        .hasFieldOrPropertyWithValue("code", MemberErrorCode.SUBMANAGER_CANNOT_LEAVE);
            }

            @Test
            @DisplayName("비활성_모임의_모임장이면_탈퇴가_가능하다")
            void 비활성_모임의_모임장이면_탈퇴가_가능하다() {
                // given: BANNED 상태의 모임이라면 탈퇴 검증을 통과해야 한다
                MemberParty bannedParty = MemberParty.builder()
                        .role(Role.party_MANAGER)
                        .status(MemberPartyStatus.BANNED)
                        .joinedAt(LocalDateTime.now())
                        .build();
                normalMember.getMemberParties().add(bannedParty);

                given(memberRepository.findById(normalMember.getId()))
                        .willReturn(Optional.of(normalMember));

                // when
                memberCommandService.withdrawMember(normalMember.getId());

                // then: 예외 없이 탈퇴 처리됨
                assertThat(normalMember.getIsActive()).isEqualTo(MemberStatus.INACTIVE);
            }
        }
    }
}
