package umc.cockple.demo.global.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DomainType {
    CONTEST("contest"),
    PROFILE("profile"),
    CHAT("chat"),
    PARTY("party");

    private final String directory;
}
