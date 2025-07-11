package umc.cockple.demo.domain.contest.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import umc.cockple.demo.domain.contest.converter.ContestConverter;
import umc.cockple.demo.domain.contest.domain.Contest;
import umc.cockple.demo.domain.contest.domain.ContestImg;
import umc.cockple.demo.domain.contest.domain.ContestVideo;
import umc.cockple.demo.domain.contest.dto.ContestRecordCreateCommand;
import umc.cockple.demo.domain.contest.dto.ContestRecordCreateRequestDTO;
import umc.cockple.demo.domain.contest.dto.ContestRecordCreateResponseDTO;
import umc.cockple.demo.domain.contest.repository.ContestRepository;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.global.s3.ImageService;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ContestCommandServiceImpl implements ContestCommandService {

    private final ContestRepository contestRepository;
    private final MemberRepository memberRepository;
    private final ContestConverter contestConverter;
    private final ImageService imageService; //이미지 업로드 담당

    //생성
    @Override
    public ContestRecordCreateResponseDTO createContestRecord(
            Long memberId, List<MultipartFile> contestImgs, ContestRecordCreateRequestDTO request) {
        /*
        1.	memberId로 Member 조회
        2.	DTO → Command 변환 (Converter 사용)
        3.	Contest 엔티티 생성
        4.	ContestImg 엔티티 생성 및 Contest에 연결
        5.	ContestVideo 엔티티 생성 (URL만 있음)
        6.	Contest 저장
        7.	ResponseDTO로 변환해서 반환
*/
        log.info("[대회 기록 등록 시작] - memberId: {}, 대회명: {}", memberId, request.contestName());

        // 1. 회원 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원이 존재하지 않습니다"));

        //2. DTO -> Command
        ContestRecordCreateCommand contestRecordCommand = contestConverter.toCreateCommand(request, memberId, contestImgs);

        log.info("받은 공개 여부 - content: {}, video: {}", request.contentIsOpen(), request.videoIsOpen());


        // 3. Command → Contest Entity 생성
        Contest contest = Contest.create(contestRecordCommand, member);

        // 4. 이미지 업로드 -> ContestImg로 변환
        if (contestRecordCommand.contestImgs() != null) {
            List<String> imageUrls = imageService.uploadImages(contestRecordCommand.contestImgs());

            for (int i = 0; i < imageUrls.size(); i++) {
                String imgUrl = imageUrls.get(i);
                String uniqueKey = UUID.randomUUID().toString(); // 나중에 파일명 대체 가능


                ContestImg contestImg = ContestImg.builder()
                        .contest(contest)
                        .imgUrl(imgUrl)
                        .imgKey(uniqueKey) // 임시 키
                        .imgOrder(i)
                        .build();

                contest.addContestImg(contestImg);
            }
        }

        // 5. 영상 URL -> ContestVideo로 변환
        if (contestRecordCommand.contestVideos() != null) {
            for (int i = 0; i < contestRecordCommand.contestVideos().size(); i++) {
                String videoUrl = contestRecordCommand.contestVideos().get(i);

                ContestVideo contestVideo = ContestVideo.builder()
                        .contest(contest)
                        .videoUrl(videoUrl)
                        .videoOrder(i)
                        .build();

                contest.getContestVideos().add(contestVideo);
            }
        }

        // 6. 저장
        Contest savedContest = contestRepository.save(contest);

        log.info("대회 기록 등록 완료 - contestId: {}", savedContest.getId());

        // 7. 응답 변환
        return contestConverter.toCreateResponseDTO(savedContest);
    }

    // todo: 수정

    // todo: 삭제
}

