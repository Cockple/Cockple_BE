package umc.cockple.demo.global.oauth2.domain;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import umc.cockple.demo.global.oauth2.domain.info.KakaoClientInfo;

@Component
@Slf4j
@RequiredArgsConstructor
public class KakaoClient {

    private final WebClient webClient = WebClient.create();

    @Value("${kakao.client-id}")
    private String clientId;

    @Value("${kakao.client-secret:}")
    private String clientSecret;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    @Value("${kakao.token-uri}")
    private String tokenUri;

    @Value("${kakao.user-info-uri}")
    private String userInfoUri;


    // 인가코드로 AccessToken 요청하기
    public String getAccessToken(String code) {
        BodyInserters.FormInserter<String> formData = BodyInserters.fromFormData("grant_type", "authorization_code")
                .with("client_id", clientId)
                .with("redirect_uri", redirectUri)
                .with("code", code);

        if (clientSecret != null && !clientSecret.isBlank()) {
            formData.with("client_secret", clientSecret);
        }

        log.info("Redirect URI used: {}", redirectUri);
        log.info("Auth Code: {}", code);
        JsonNode response = webClient.post()
                .uri(tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        return response.get("access_token").asText();
    }

    // access 토큰으로 사용자 정보 요청
    public KakaoClientInfo getClientInfo(String accessToken) {
        JsonNode response = webClient.get()
                .uri(userInfoUri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        Long kakaoId = response.get("id").asLong();
        String nickname = response.path("properties").path("nickname").asText();

        return new KakaoClientInfo(kakaoId, nickname);
    }


}
