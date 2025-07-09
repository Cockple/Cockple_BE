package umc.cockple.demo.domain.party.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.party.converter.PartyConverter;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.domain.PartyAddr;
import umc.cockple.demo.domain.party.dto.PartyAddrCreateCommand;
import umc.cockple.demo.domain.party.dto.PartyCreateCommand;
import umc.cockple.demo.domain.party.dto.PartyCreateRequestDTO;
import umc.cockple.demo.domain.party.dto.PartyCreateResponseDTO;
import umc.cockple.demo.domain.party.exception.MemberErrorCode;
import umc.cockple.demo.domain.party.exception.MemberException;
import umc.cockple.demo.domain.party.repository.PartyAddrRepository;
import umc.cockple.demo.domain.party.repository.PartyRepository;
import umc.cockple.demo.global.s3.ImageService;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PartyCommandServiceImpl implements PartyCommandService{

    private final PartyRepository partyRepository;
    private final PartyAddrRepository partyAddrRepository;
    private final MemberRepository memberRepository;
    private final PartyConverter partyConverter;
    private final ImageService imageService;

    @Override
    public PartyCreateResponseDTO createParty(Long memberId, MultipartFile profileImage, PartyCreateRequestDTO request) {
        log.info("모임 생성 시작 - memberId: {}", memberId);

        //1. DTO -> Command 객체로 변환
        PartyCreateCommand partyCommand = partyConverter.toCreateCommand(request);
        PartyAddrCreateCommand addrCommand = partyConverter.toAddrCreateCommand(request);

        //2. 모임장이 될 사용자 조회
        Member owner = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        //3. 주소 처리 (조회 또는 새로 생성)
        PartyAddr partyAddr = findOrCreatePartyAddr(addrCommand);

        //4. 이미지 업로드 처리
        String imageUrl = imageService.uploadImage(profileImage);

        //5. Party 엔티티 생성
        Party newParty = Party.create(partyCommand, partyAddr, imageUrl, owner);

        //6. DB에 Party 저장 (cascade 설정으로 연관 엔티티 모두 자동 저장)
        Party savedParty = partyRepository.save(newParty);

        log.info("모임 생성 완료 - 모임ID: {}", savedParty.getId());

        // 7. ResponseDTO로 변환하여 반환
        return partyConverter.toCreateResponseDTO(savedParty);
    }

    //주소가 이미 존재하면 조회, 없으면 새로 생성하여 저장
    private PartyAddr findOrCreatePartyAddr(PartyAddrCreateCommand command) {
        return partyAddrRepository.findByAddr1AndAddr2(command.addr1(), command.addr2())
                .orElseGet(() -> {
                    PartyAddr newAddr = PartyAddr.create(command.addr1(), command.addr2());
                    return partyAddrRepository.save(newAddr);
                });
    }
}