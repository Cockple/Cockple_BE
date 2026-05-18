package umc.cockple.demo.support.fixture;

import umc.cockple.demo.domain.contest.domain.Contest;
import umc.cockple.demo.domain.contest.enums.MedalType;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.party.enums.ParticipationType;
import umc.cockple.demo.global.enums.Level;

import java.time.LocalDate;
import java.util.ArrayList;

public class ContestFixture {

    public static Contest createContest(Member member, String contestName, MedalType medalType) {
        return Contest.builder()
                .member(member)
                .contestName(contestName)
                .medalType(medalType)
                .date(LocalDate.of(2024, 6, 15))
                .type(ParticipationType.SINGLE)
                .level(Level.A)
                .content("대회 후기입니다.")
                .contentIsOpen(true)
                .videoIsOpen(true)
                .contestImgs(new ArrayList<>())
                .contestVideos(new ArrayList<>())
                .build();
    }

    public static Contest createContest(Member member, String contestName, MedalType medalType,
                                        LocalDate date, ParticipationType type, Level level) {
        return Contest.builder()
                .member(member)
                .contestName(contestName)
                .medalType(medalType)
                .date(date)
                .type(type)
                .level(level)
                .content("대회 후기입니다.")
                .contentIsOpen(true)
                .videoIsOpen(true)
                .contestImgs(new ArrayList<>())
                .contestVideos(new ArrayList<>())
                .build();
    }

    public static Contest createPrivateContest(Member member, String contestName, MedalType medalType,
                                               LocalDate date, ParticipationType type, Level level,
                                               String content) {
        return Contest.builder()
                .member(member)
                .contestName(contestName)
                .medalType(medalType)
                .date(date)
                .type(type)
                .level(level)
                .content(content)
                .contentIsOpen(false)
                .videoIsOpen(false)
                .contestImgs(new ArrayList<>())
                .contestVideos(new ArrayList<>())
                .build();
    }
}
