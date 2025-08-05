package umc.cockple.demo.domain.exercise.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.exercise.dto.ExerciseCreateDTO;
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.exception.ExerciseException;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.repository.PartyRepository;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ExerciseCommandService {

    private final ExerciseLifecycleService exerciseLifecycleService;

    private final PartyRepository partyRepository;
    private final MemberRepository memberRepository;

    public ExerciseCreateDTO.Response createExercise(Long partyId, Long memberId, ExerciseCreateDTO.Request request) {
        log.info("운동 생성 시작 - partyId: {}, memberId: {}, date: {}", partyId, memberId, request.date());

        Party party = findPartyOrThrow(partyId);
        Member member = findMemberOrThrow(memberId);

        return exerciseLifecycleService.createExercise(party, member, request);
    }

    // ========== 조회 메서드 ==========

    private Member findMemberOrThrow(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new ExerciseException(ExerciseErrorCode.MEMBER_NOT_FOUND));
    }

    private Party findPartyOrThrow(Long partyId) {
        return partyRepository.findById(partyId)
                .orElseThrow(() -> new ExerciseException(ExerciseErrorCode.PARTY_NOT_FOUND));
    }
}
