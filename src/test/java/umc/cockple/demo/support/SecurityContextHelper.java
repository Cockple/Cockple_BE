package umc.cockple.demo.support;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import umc.cockple.demo.global.security.domain.CustomUserDetails;

public class SecurityContextHelper {

    public static void setAuthentication(Long memberId, String nickname) {
        CustomUserDetails userDetails = new CustomUserDetails(memberId, nickname);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    public static void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }
}
