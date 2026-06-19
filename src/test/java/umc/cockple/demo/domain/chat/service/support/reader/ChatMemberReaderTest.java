package umc.cockple.demo.domain.chat.service.support.reader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.cockple.demo.domain.chat.exception.ChatErrorCode;
import umc.cockple.demo.domain.chat.exception.ChatException;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.fixture.MemberFixture;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatMemberReader")
class ChatMemberReaderTest {

    @Mock private MemberRepository memberRepository;

    private ChatMemberReader chatMemberReader;

    @BeforeEach
    void setUp() {
        chatMemberReader = new ChatMemberReader(memberRepository);
    }

    @Test
    @DisplayName("프로필 이미지와 함께 회원을 조회한다")
    void readWithProfile_returnsMember() {
        // given
        Long memberId = 1L;
        Member member = MemberFixture.createMemberWithName("홍길동", "길동", Gender.MALE, Level.A, 1001L);
        given(memberRepository.findMemberWithProfileById(memberId)).willReturn(Optional.of(member));

        // when
        Member result = chatMemberReader.readWithProfile(memberId);

        // then
        assertThat(result).isSameAs(member);
    }

    @Test
    @DisplayName("회원 조회 결과가 없으면 MEMBER_NOT_FOUND 예외를 던진다")
    void readWithProfile_throwsWhenMemberNotFound() {
        // given
        Long memberId = 1L;
        given(memberRepository.findMemberWithProfileById(memberId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> chatMemberReader.readWithProfile(memberId))
                .isInstanceOfSatisfying(ChatException.class, exception ->
                        assertThat(exception.getErrorReason().getCode())
                                .isEqualTo(ChatErrorCode.MEMBER_NOT_FOUND.getCode()));
    }
}
