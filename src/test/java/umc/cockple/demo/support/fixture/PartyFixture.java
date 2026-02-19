package umc.cockple.demo.support.fixture;

import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.domain.PartyAddr;
import umc.cockple.demo.domain.party.enums.ActivityTime;
import umc.cockple.demo.domain.party.enums.ParticipationType;
import umc.cockple.demo.domain.party.enums.PartyStatus;

public class PartyFixture {

    public static PartyAddr createPartyAddr(String addr1, String addr2) {
        return PartyAddr.builder()
                .addr1(addr1)
                .addr2(addr2)
                .build();
    }

    public static Party createParty(String partyName, Long ownerId, PartyAddr addr) {
        return Party.builder()
                .partyName(partyName)
                .ownerId(ownerId)
                .partyAddr(addr)
                .partyType(ParticipationType.MIX_DOUBLES)
                .minBirthYear(1990)
                .maxBirthYear(2005)
                .price(10000)
                .joinPrice(5000)
                .designatedCock("욘넥스")
                .activityTime(ActivityTime.MORNING)
                .status(PartyStatus.ACTIVE)
                .exerciseCount(0)
                .build();
    }
}
