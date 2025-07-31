package umc.cockple.demo.global.security.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.exception.MemberException;
import umc.cockple.demo.global.security.domain.CustomUserDetails;

import java.util.Optional;

public class SecurityUtil {

    /**
     * 현재 로그인한 사용자의 memberId를 반환
     */
    public static Long getCurrentMemberId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new MemberException(MemberErrorCode.MEMBER_NOT_FOUND);
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof CustomUserDetails)) {
            throw new MemberException(MemberErrorCode.MEMBER_NOT_FOUND);
        }

        return ((CustomUserDetails) principal).getMemberId();
    }

}
