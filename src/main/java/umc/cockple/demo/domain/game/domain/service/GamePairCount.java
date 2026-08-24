package umc.cockple.demo.domain.game.domain.service;

public record GamePairCount(
        Long memberIdA,
        Long memberIdB,
        long count) {
}
