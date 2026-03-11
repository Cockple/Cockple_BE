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
import umc.cockple.demo.domain.contest.domain.Contest;
import umc.cockple.demo.domain.contest.enums.MedalType;
import umc.cockple.demo.domain.file.service.FileService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberAddr;
import umc.cockple.demo.domain.member.domain.MemberExercise;
import umc.cockple.demo.domain.member.domain.ProfileImg;
import umc.cockple.demo.domain.member.dto.GetAllAddressResponseDTO;
import umc.cockple.demo.domain.member.dto.GetMyProfileResponseDTO;
import umc.cockple.demo.domain.member.dto.GetNowAddressResponseDTO;
import umc.cockple.demo.domain.member.dto.GetProfileResponseDTO;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.exception.MemberException;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Keyword;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.fixture.MemberAddrFixture;
import umc.cockple.demo.support.fixture.MemberFixture;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberQueryService")
class MemberQueryServiceTest {

    @InjectMocks
    private MemberQueryService memberQueryService;

    @Mock private MemberRepository memberRepository;
    @Mock private FileService fileService;

    private Member member;

    @BeforeEach
    void setUp() {
        member = MemberFixture.createMember("강와나", Gender.FEMALE, Level.A, 1001L);
        ReflectionTestUtils.setField(member, "id", 1L);
    }

    @Nested
    @DisplayName("getProfile")
    class GetProfile {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("profileImg가_있으면_fileService로_url을_변환해서_반환한다")
            void profileImg가_있으면_fileService로_url을_변환해서_반환한다() {
                // given
                ProfileImg profileImg = ProfileImg.builder()
                        .imgKey("profile/test-key.jpg")
                        .build();
                ReflectionTestUtils.setField(member, "profileImg", profileImg);

                given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));
                given(fileService.getUrlFromKey("profile/test-key.jpg"))
                        .willReturn("https://cdn.example.com/profile/test-key.jpg");

                // when
                GetProfileResponseDTO response = memberQueryService.getProfile(member.getId());

                // then
                assertThat(response.profileImgUrl()).isEqualTo("https://cdn.example.com/profile/test-key.jpg");
                then(fileService).should().getUrlFromKey("profile/test-key.jpg");
            }

            @Test
            @DisplayName("profileImg가_없으면_imgUrl이_null로_반환된다")
            void profileImg가_없으면_imgUrl이_null로_반환된다() {
                // given
                given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));

                // when
                GetProfileResponseDTO response = memberQueryService.getProfile(member.getId());

                // then
                assertThat(response.profileImgUrl()).isNull();
                then(fileService).should(never()).getUrlFromKey(org.mockito.ArgumentMatchers.any());
            }

            @Test
            @DisplayName("금_은_동_메달_개수가_올바르게_집계된다")
            void 금_은_동_메달_개수가_올바르게_집계된다() {
                // given
                Contest gold1 = Contest.builder().medalType(MedalType.GOLD).build();
                Contest gold2 = Contest.builder().medalType(MedalType.GOLD).build();
                Contest silver = Contest.builder().medalType(MedalType.SILVER).build();
                Contest bronze = Contest.builder().medalType(MedalType.BRONZE).build();

                member.getContests().addAll(List.of(gold1, gold2, silver, bronze));

                given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));

                // when
                GetProfileResponseDTO response = memberQueryService.getProfile(member.getId());

                // then
                assertThat(response.myGoldMedalCnt()).isEqualTo(2);
                assertThat(response.mySilverMedalCnt()).isEqualTo(1);
                assertThat(response.myBronzeMedalCnt()).isEqualTo(1);
            }

            @Test
            @DisplayName("참여한_모임_수가_올바르게_반환된다")
            void 참여한_모임_수가_올바르게_반환된다() {
                // given
                member.getMemberParties().add(MemberFixture.createMemberParty(null, member, umc.cockple.demo.global.enums.Role.party_MEMBER));
                member.getMemberParties().add(MemberFixture.createMemberParty(null, member, umc.cockple.demo.global.enums.Role.party_MEMBER));

                given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));

                // when
                GetProfileResponseDTO response = memberQueryService.getProfile(member.getId());

                // then
                assertThat(response.myPartyCnt()).isEqualTo(2);
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
                assertThatThrownBy(() -> memberQueryService.getProfile(999L))
                        .isInstanceOf(MemberException.class)
                        .hasFieldOrPropertyWithValue("code", MemberErrorCode.MEMBER_NOT_FOUND);
            }
        }
    }


    @Nested
    @DisplayName("getMyProfile")
    class GetMyProfile {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("대표주소_운동횟수_키워드가_포함된_내_프로필이_반환된다")
            void 대표주소_운동횟수_키워드가_포함된_내_프로필이_반환된다() {
                // given
                MemberAddr mainAddr = MemberAddrFixture.createAddr(member, "역삼동", "서울특별시 강남구 테헤란로 1", true);
                member.getAddresses().add(mainAddr);

                MemberExercise exercise = MemberFixture.createMemberExercise(member, null);
                member.getMemberExercises().add(exercise);

                umc.cockple.demo.domain.member.domain.MemberKeyword keyword =
                        umc.cockple.demo.domain.member.domain.MemberKeyword.builder()
                                .member(member)
                                .keyword(Keyword.FRIENDSHIP)
                                .build();
                member.getKeywords().add(keyword);

                given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));

                // when
                GetMyProfileResponseDTO response = memberQueryService.getMyProfile(member.getId());

                // then
                assertThat(response.addr3()).isEqualTo("역삼동");
                assertThat(response.myExerciseCnt()).isEqualTo(1);
                assertThat(response.keywords()).containsExactly(Keyword.FRIENDSHIP);
            }
        }
    }


    @Nested
    @DisplayName("getNowAddress")
    class GetNowAddress {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("대표주소를_반환한다")
            void 대표주소를_반환한다() {
                // given
                MemberAddr mainAddr = MemberAddrFixture.createAddr(member, "역삼동", "서울특별시 강남구 테헤란로 1", true);
                member.getAddresses().add(mainAddr);

                given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));

                // when
                GetNowAddressResponseDTO response = memberQueryService.getNowAddress(member.getId());

                // then
                assertThat(response.addr3()).isEqualTo("역삼동");
                assertThat(response.streetAddr()).isEqualTo("서울특별시 강남구 테헤란로 1");
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
                assertThatThrownBy(() -> memberQueryService.getNowAddress(999L))
                        .isInstanceOf(MemberException.class)
                        .hasFieldOrPropertyWithValue("code", MemberErrorCode.MEMBER_NOT_FOUND);
            }

            @Test
            @DisplayName("대표주소가_없으면_MAIN_ADDRESS_NULL_예외를_던진다")
            void 대표주소가_없으면_MAIN_ADDRESS_NULL_예외를_던진다() {
                // given: isMain=false인 주소만 존재
                MemberAddr nonMainAddr = MemberAddrFixture.createAddr(member, "삼성동", "서울특별시 강남구 테헤란로 1", false);
                member.getAddresses().add(nonMainAddr);

                given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));

                // when & then
                assertThatThrownBy(() -> memberQueryService.getNowAddress(member.getId()))
                        .isInstanceOf(MemberException.class)
                        .hasFieldOrPropertyWithValue("code", MemberErrorCode.MAIN_ADDRESS_NULL);
            }
        }
    }


    @Nested
    @DisplayName("getAllAddress")
    class GetAllAddress {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("대표주소가_목록_첫_번째로_반환된다")
            void 대표주소가_목록_첫_번째로_반환된다() {
                // given
                MemberAddr nonMain = MemberAddrFixture.createAddr(member, "삼성동", "서울특별시 강남구 테헤란로 1", false);
                MemberAddr main = MemberAddrFixture.createAddr(member, "역삼동", "서울특별시 강남구 테헤란로 1", true);
                ReflectionTestUtils.setField(nonMain, "id", 1L);
                ReflectionTestUtils.setField(main, "id", 2L);

                member.getAddresses().addAll(List.of(nonMain, main)); // 비대표, 대표 순서로 추가

                given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));

                // when
                List<GetAllAddressResponseDTO> result = memberQueryService.getAllAddress(member.getId());

                // then
                assertThat(result).hasSize(2);
                assertThat(result.get(0).isMainAddr()).isTrue();
                assertThat(result.get(0).addr3()).isEqualTo("역삼동");
            }

            @Test
            @DisplayName("대표주소_제외_나머지는_id_오름차순으로_정렬된다")
            void 대표주소_제외_나머지는_id_오름차순으로_정렬된다() {
                // given
                MemberAddr main = MemberAddrFixture.createAddr(member, "역삼동", "서울특별시 강남구 테헤란로 1", true);
                MemberAddr non1 = MemberAddrFixture.createAddr(member, "삼성동", "서울특별시 강남구 테헤란로 1", false);
                MemberAddr non2 = MemberAddrFixture.createAddr(member, "청담동", "서울특별시 강남구 테헤란로 1", false);
                ReflectionTestUtils.setField(main, "id", 1L);
                ReflectionTestUtils.setField(non1, "id", 2L);
                ReflectionTestUtils.setField(non2, "id", 3L);

                member.getAddresses().addAll(List.of(non2, non1, main)); // 섞인 순서로 추가

                given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));

                // when
                List<GetAllAddressResponseDTO> result = memberQueryService.getAllAddress(member.getId());

                // then
                assertThat(result).hasSize(3);
                assertThat(result.get(0).addr3()).isEqualTo("역삼동"); // 대표주소 먼저
                assertThat(result.get(1).addr3()).isEqualTo("삼성동"); // id=2
                assertThat(result.get(2).addr3()).isEqualTo("청담동"); // id=3
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
                assertThatThrownBy(() -> memberQueryService.getAllAddress(999L))
                        .isInstanceOf(MemberException.class)
                        .hasFieldOrPropertyWithValue("code", MemberErrorCode.MEMBER_NOT_FOUND);
            }

            @Test
            @DisplayName("주소가_없으면_MEMBER_ADDRESS_MINIMUM_REQUIRED_예외를_던진다")
            void 주소가_없으면_MEMBER_ADDRESS_MINIMUM_REQUIRED_예외를_던진다() {
                // given: 주소가 비어있는 경우
                given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));

                // when & then
                assertThatThrownBy(() -> memberQueryService.getAllAddress(member.getId()))
                        .isInstanceOf(MemberException.class)
                        .hasFieldOrPropertyWithValue("code", MemberErrorCode.MEMBER_ADDRESS_MINIMUM_REQUIRED);
            }
        }
    }
}
