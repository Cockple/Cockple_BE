package umc.cockple.demo.domain.party.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.exception.PartyErrorCode;
import umc.cockple.demo.domain.party.exception.PartyException;
import umc.cockple.demo.domain.party.repository.PartyRepository;
import umc.cockple.demo.domain.party.service.query.lookup.PartyLookupService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("PartyLookupService")
class PartyLookupServiceTest {

    @InjectMocks
    private PartyLookupService partyLookupService;

    @Mock private PartyRepository partyRepository;
    @Mock private Party party;

    @Test
    @DisplayName("모임 ID로 모임을 조회한다")
    void findByIdOrThrow_returnsParty() {
        given(partyRepository.findById(1L)).willReturn(Optional.of(party));

        assertThat(partyLookupService.findByIdOrThrow(1L)).isSameAs(party);
    }

    @Test
    @DisplayName("모임이 없으면 PartyException(PARTY_NOT_FOUND)을 던진다")
    void findByIdOrThrow_throwsWhenMissing() {
        given(partyRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> partyLookupService.findByIdOrThrow(1L))
                .isInstanceOf(PartyException.class)
                .satisfies(exception -> assertThat(((PartyException) exception).getCode())
                        .isEqualTo(PartyErrorCode.PARTY_NOT_FOUND));
    }
}
