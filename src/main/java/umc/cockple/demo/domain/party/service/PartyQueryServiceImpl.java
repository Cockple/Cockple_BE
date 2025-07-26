package umc.cockple.demo.domain.party.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberParty;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.exception.MemberException;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.party.converter.PartyConverter;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.domain.PartyJoinRequest;
import umc.cockple.demo.domain.party.dto.*;
import umc.cockple.demo.domain.party.exception.PartyErrorCode;
import umc.cockple.demo.domain.party.exception.PartyException;
import umc.cockple.demo.domain.party.repository.PartyJoinRequestRepository;
import umc.cockple.demo.domain.party.repository.PartyRepository;
import umc.cockple.demo.global.enums.PartyOrderType;
import umc.cockple.demo.global.enums.PartyStatus;
import umc.cockple.demo.global.enums.RequestStatus;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
//조회 용 서비스이기에 readOnly = true를 추가하여 성능 향상했습니다.
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class PartyQueryServiceImpl implements PartyQueryService{
    private final PartyRepository partyRepository;
    private final PartyJoinRequestRepository partyJoinRequestRepository;
    private final PartyConverter partyConverter;
    private final MemberRepository memberRepository;
    private final MemberPartyRepository memberPartyRepository;
    private final ExerciseRepository exerciseRepository;

    @Override
    public Slice<PartySimpleDTO.Response> getSimpleMyParties(Long memberId, Pageable pageable) {
        log.info("내 모임 간략화 조회 시작 - memberId: {}", memberId);
        //사용자 조회
        Member member = findMemberOrThrow(memberId);
        //memberParty 조회 로직 수행
        Slice<MemberParty> memberPartySlice = memberPartyRepository.findByMember(member, pageable);

        log.info("내 모임 간략화 목록 조회 완료. 조회된 항목 수: {}", memberPartySlice.getNumberOfElements());
        return memberPartySlice.map(partyConverter::toPartySimpleDTO);
    }

    @Override
    public Slice<PartyDTO.Response> getMyParties(Long memberId, Boolean created, String sort, Pageable pageable) {
        log.info("내 모임 조회 시작 - memberId: {}", memberId);

        //정렬 기준 문자 검증, Pageable 객체 생성
        Pageable sortedPageable = createSortedPageable(pageable, sort);

        //모임 정보 조회
        Slice<Party> partySlice = partyRepository.findMyParty(memberId, created, sortedPageable);
        List<Long> partyIds = partySlice.getContent().stream().map(Party::getId).toList();
        //운동 정보 조회
        ExerciseInfo exerciseInfo = getExerciseInfo(partyIds);

        log.info("내 모임 목록 조회 완료. 조회된 항목 수: {}", partySlice.getNumberOfElements());

        //기본 정보와 추가 정보를 조합하여 최종 DTO 생성
        return partySlice.map(party -> {
            Integer totalExerciseCount = exerciseInfo.countMap().getOrDefault(party.getId(), 0);
            String nextExerciseInfo = exerciseInfo.nextInfoMap().get(party.getId());
            return partyConverter.toMyPartyDTO(party, nextExerciseInfo, totalExerciseCount);
        });
    }

    @Override
    public PartyMemberDTO.Response getPartyMembers(Long partyId, Long currentMemberId) {
        log.info("모임 멤버 조회 시작 - partyId: {}", partyId);
        //모임 조회
        Party party = findPartyOrThrow(partyId);

        //모임 활성화 검증
        validatePartyIsActive(party);
        //모임 멤버 목록 조회
        List<MemberParty> memberParties = memberPartyRepository.findAllByPartyIdWithMember(partyId);

        log.info("모임 멤버 목록 조회 완료 - partyId: {}", partyId);
        return partyConverter.toPartyMemberDTO(memberParties, currentMemberId);
    }

    @Override
    public PartyDetailDTO.Response getPartyDetails(Long partyId, Long memberId) {
        log.info("모임 상세 정보 조회 시작 - partyId: {}, memberId: {}", partyId, memberId);

        //모임, 사용자 조회
        Party party = findPartyOrThrow(partyId);
        Member member = findMemberOrThrow(memberId);

        //모임 활성화 검증
        validatePartyIsActive(party);

        //memberParty 조회 로직 수행
        Optional<MemberParty> memberParty = memberPartyRepository.findByPartyAndMember(party, member);

        PartyDetailDTO.Response response = partyConverter.toPartyDetailResponseDTO(party, memberParty);

        log.info("모임 상세 정보 조회 완료 - partyId: {}", partyId);
        return response;
    }

    @Override
    public Slice<PartyJoinDTO.Response> getJoinRequests(Long partyId, Long memberId, String status, Pageable pageable) {
        log.info("가입 신청 목록 조회 시작 - partyId: {}, memberId: {}", partyId, memberId);

        //모임 조회
        Party party = findPartyOrThrow(partyId);

        //모임 활성화 검증
        validatePartyIsActive(party);
        //모임장 권한이 있는지 검증
        validateOwnerPermission(party, memberId);
        //status를 ENUM으로 변환 및 검증
        RequestStatus requestStatus = parseRequestStatus(status);

        //조회 로직 수행
        Slice<PartyJoinRequest> requestSlice = partyJoinRequestRepository
                .findByPartyAndStatus(party, requestStatus, pageable);

        log.info("가입 신청 목록 조회 완료. 조회된 항목 수: {}", requestSlice.getNumberOfElements());
        return requestSlice.map(partyConverter::toPartyJoinResponseDTO);
    }

    // ========== 조회 메서드 ==========
    //사용자 조회
    private Party findPartyOrThrow(Long partyId) {
        return partyRepository.findById(partyId)
                .orElseThrow(() -> new PartyException(PartyErrorCode.PARTY_NOT_FOUND));
    }

    //멤버 조회
    private Member findMemberOrThrow(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
    }

    //운동 정보 조회
    private ExerciseInfo getExerciseInfo(List<Long> partyIds) {
        //운동 개수 정보 조회
        Map<Long, Integer> exerciseCountMap = exerciseRepository.findTotalExerciseCountsByPartyIds(partyIds)
                .stream()
                .collect(Collectors.toMap(PartyExerciseInfoDTO::partyId, dto -> dto.count().intValue()));

        //가장 최신의 운동 정보 조회
        Map<Long, String> nextExerciseInfoMap = exerciseRepository.findUpcomingExercisesByPartyIds(partyIds)
                .stream()
                .collect(Collectors.toMap(
                        exercise -> exercise.getParty().getId(),
                        this::formatNextExerciseInfo,
                        (existing, replacement) -> existing //key중복 시 처리 방법: 최초 값(existing)만 사용
                ));

        return new ExerciseInfo(exerciseCountMap, nextExerciseInfoMap);
    }

    // ========== 검증 메서드 ==========
    //모임장 권한 검증
    private void validateOwnerPermission(Party party, Long memberId) {
        if(!party.getOwnerId().equals(memberId)){
            throw new PartyException(PartyErrorCode.INSUFFICIENT_PERMISSION);
        }
    }

    //모임 활성화 검증
    private void validatePartyIsActive(Party party) {
        if (party.getStatus() == PartyStatus.INACTIVE) {
            throw new PartyException(PartyErrorCode.PARTY_IS_DELETED);
        }
    }

    // ========== 비즈니스 로직 메서드 ==========
    //정렬 로직 처리
    private Pageable createSortedPageable(Pageable pageable, String sort) {
        PartyOrderType sortType = PartyOrderType.fromKorean(sort);

        Sort sorting = switch (sortType) {
            case OLDEST -> Sort.by("createdAt").ascending();
            case EXERCISE_COUNT -> Sort.by("exerciseCount").descending();
            default -> Sort.by("createdAt").descending(); //기본값은 LATEST (createdAt 내림차순)
        };
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sorting);
    }

    // ========== 데이터 변환 메서드 ==========
    //가입신청 상태 변환
    private RequestStatus parseRequestStatus(String status) {
        try {
            return RequestStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new PartyException(PartyErrorCode.INVALID_REQUEST_STATUS);
        }
    }

    //운동 정보 변환
    private String formatNextExerciseInfo(Exercise exercise) {
        //날짜 포맷팅 ex) 05.01
        String datePart = exercise.getDate().format(DateTimeFormatter.ofPattern("MM.dd"));

        //시간 포맷팅 (오전/오후)
        String timePart = convertToTimeOfDay(exercise.getStartTime());

        return datePart + " " + timePart + " 운동";
    }

    //LocalTime을 오전/오후로 변환
    private String convertToTimeOfDay(LocalTime time) {
        int hour = time.getHour();
        if (hour >= 0 && hour < 12) {
            return "오전";
        } else{
            return "오후";
        }
    }

    // ========== record ==========
    //임시로 사용할 데이터 묶음을 record로 구현
    private record ExerciseInfo(Map<Long, Integer> countMap, Map<Long, String> nextInfoMap) {}
}
