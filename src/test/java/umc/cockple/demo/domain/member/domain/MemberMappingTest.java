package umc.cockple.demo.domain.member.domain;

import jakarta.persistence.OneToMany;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import umc.cockple.demo.domain.chat.domain.ChatRoomMember;

import java.lang.reflect.Field;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Member JPA mapping")
class MemberMappingTest {

    @Test
    @DisplayName("chatRoomMembers는 회원 삭제 시 채팅방 참여 이력을 삭제하지 않도록 cascade와 orphanRemoval을 사용하지 않는다")
    void chatRoomMembers_doesNotCascadeOrphanRemoval() throws NoSuchFieldException {
        Field field = Member.class.getDeclaredField("chatRoomMembers");
        OneToMany oneToMany = field.getAnnotation(OneToMany.class);

        assertThat(oneToMany).isNotNull();
        assertThat(oneToMany.mappedBy()).isEqualTo("member");
        assertThat(field.getGenericType().getTypeName()).contains(ChatRoomMember.class.getSimpleName());
        assertThat(Arrays.asList(oneToMany.cascade())).isEmpty();
        assertThat(oneToMany.orphanRemoval()).isFalse();
    }
}
