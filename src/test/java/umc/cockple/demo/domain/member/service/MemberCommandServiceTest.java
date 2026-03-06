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
import umc.cockple.demo.domain.image.service.ImageService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.repository.*;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.global.oauth2.service.KakaoOauthService;
import umc.cockple.demo.support.fixture.MemberFixture;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

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
    @Mock private KakaoOauthService kakaoOauthService;
    @Mock private ImageService imageService;

    private Member normalMember;

    @BeforeEach
    void setUp() {
        normalMember = MemberFixture.createMember("일반멤버", Gender.MALE, Level.C, 9001L);
        ReflectionTestUtils.setField(normalMember, "id", 1L);
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
    }
}
