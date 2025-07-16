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
import umc.cockple.demo.domain.contest.dto.*;
import umc.cockple.demo.domain.contest.exception.ContestErrorCode;
import umc.cockple.demo.domain.contest.exception.ContestException;
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

            /*
        1.	memberId로 Member 조회
        2.	DTO → Command 변환 (Converter 사용)
        3.	Contest 엔티티 생성
        4.	ContestImg 엔티티 생성 및 Contest에 연결
        5.	ContestVideo 엔티티 생성 (URL만 있음)
        6.	Contest 저장
        7.	ResponseDTO로 변환해서 반환
*/

    // 등록
    @Override
    public ContestRecordCreateDTO.Response createContestRecord(
            Long memberId, List<MultipartFile> contestImgs, ContestRecordCreateDTO.Request request) {

        log.info("[대회 기록 등록 시작] - memberId: {}, 대회명: {}", memberId, request.contestName());

        // 1. 회원 조회
        Member member = getMember(memberId);

        //2. DTO -> Command
        ContestRecordCreateDTO.Command contestRecordCommand = contestConverter.toCreateCommand(request, memberId, contestImgs);

        // 3. Command → Contest Entity 생성
        Contest contest = Contest.create(contestRecordCommand, member);

        // 4. 이미지 업로드 -> ContestImg로 변환
        try {
            extractedImg(contestRecordCommand, contest);
        } catch (Exception e) {
            log.error("이미지 업로드 중 예외 발생", e);
            throw new ContestException(ContestErrorCode.IMAGE_UPLOAD_FAIL);
        }

        // 5. 영상 URL -> ContestVideo로 변환
        try {
            extractedVideo(contestRecordCommand, contest);
        } catch (Exception e) {
            log.error("영상 URL 처리 중 예외 발생", e);
            throw new ContestException(ContestErrorCode.VIDEO_URL_SAVE_FAIL);
        }

        // 6. 저장
        Contest savedContest;
        try {
            savedContest = contestRepository.save(contest);
        } catch (Exception e) {
            log.error("대회 기록 저장 중 예외 발생", e);
            throw new ContestException(ContestErrorCode.CONTEST_SAVE_FAIL);
        }

        log.info("대회 기록 등록 완료 - contestId: {}", savedContest.getId());

        // 7. 응답 변환
        return contestConverter.toCreateResponse(savedContest);
    }

    // 수정
    @Override
    public ContestRecordUpdateDTO.Response updateContestRecord(
            Long memberId, Long contestId, List<MultipartFile> contestImgs, ContestRecordUpdateDTO.Request request
    ) {

        log.info("[대회 기록 수정 시작] - memberId: {}, contestId: {}", memberId, contestId);

        Contest contest = contestRepository.findByIdAndMember_Id(contestId, memberId)
                .orElseThrow(() -> new ContestException(ContestErrorCode.CONTEST_NOT_FOUND));

        // 2. 기본 필드 수정
        contest.updateFromRequest(request);

        // 3. 이미지 삭제
        if (request.contestImgsToDelete() != null) {
            request.contestImgsToDelete().forEach(imgKey -> {
                contest.getContestImgs().removeIf(img -> img.getImgKey().equals(imgKey));
                imageService.delete(imgKey);
            });
        }

        // 4. 이미지 재정렬
        List<ContestImg> imgs = contest.getContestImgs();
        for (int i = 0; i < imgs.size(); i++) {
            imgs.get(i).setImgOrder(i);
        }

        // 5. 이미지 추가
        try {
            extractedImg(contestImgs, contest);
        } catch (Exception e) {
            log.error("이미지 업로드 중 예외 발생", e);
            throw new ContestException(ContestErrorCode.IMAGE_UPLOAD_FAIL);
        }

        // 6. 영상 삭제
        if (request.contestVideoIdsToDelete() != null) {
            request.contestVideoIdsToDelete().forEach(id ->
                    contest.getContestVideos().removeIf(video -> video.getId().equals(id))
            );
        }

        // 7. order 재정렬
        List<ContestVideo> videos = contest.getContestVideos();
        for (int i = 0; i < videos.size(); i++) {
            videos.get(i).setVideoOrder(i);
        }

        // 8. 영상 추가
        try {
            extractedVideo(request, contest);
        } catch (Exception e) {
            log.error("영상 수정 중 오류 발생", e);
            throw new ContestException(ContestErrorCode.VIDEO_URL_SAVE_FAIL);
        }

        // 9. 저장
        try {
            contestRepository.save(contest);
        } catch (Exception e) {
            log.error("대회 기록 수정 저장 중 예외 발생", e);
            throw new ContestException(ContestErrorCode.CONTEST_SAVE_FAIL);
        }

        log.info("대회 기록 수정 완료 - contestId: {}", contestId);

        return contestConverter.toUpdateResponseDTO(contest);
    }

    // 삭제
    @Override
    public ContestRecordDeleteDTO.Response deleteContestRecord(Long memberId, Long contestId) {

        log.info("[대회 기록 삭제 시작] - memberId: {}, contestId: {}", memberId, contestId);

        // 1. 대회 조회
        Contest contest = contestRepository.findByIdAndMember_Id(contestId, memberId)
                .orElseThrow(() -> new ContestException(ContestErrorCode.CONTEST_NOT_FOUND));

        // 2. 연관관계 해제 (양방향)
        contest.removeMember();

        // 3. 삭제
        contestRepository.delete(contest);

        log.info("대회 기록 삭제 완료 - contestId: {}", contestId);

        return contestConverter.toDeleteResponseDTO(contest);
    }

    private static void extractedVideo(ContestRecordUpdateDTO.Request request, Contest contest) {
        if (request.contestVideos() != null) {
            int maxOrder = contest.getContestVideos().stream()
                    .mapToInt(ContestVideo::getVideoOrder)
                    .max().orElse(-1);

            for (int i = 0; i < request.contestVideos().size(); i++) {
                String videoUrl = request.contestVideos().get(i);
                ContestVideo video = ContestVideo.of(contest, videoUrl, maxOrder + i + 1);
                contest.getContestVideos().add(video);
            }
        }
    }

    private void extractedImg(List<MultipartFile> contestImgs, Contest contest) {
        if (contestImgs != null && !contestImgs.isEmpty()) {
            int baseOrder = contest.getContestImgs().size();

            // 실제 업로드된 URL들 (null 아닌 값만 들어옴)
            List<String> imgUrls = imageService.uploadImages(contestImgs);

            int totalImages = contest.getContestImgs().size() + contestImgs.size();
            if (totalImages > 3) {
                log.error("이미지 최대 개수 초과 예외");
                throw new ContestException(ContestErrorCode.IMAGE_UPLOAD_LIMIT_EXCEEDED);
            }

            for (int i = 0; i < imgUrls.size(); i++) {
                String imgUrl = imgUrls.get(i);
                String imgKey = UUID.randomUUID().toString(); // 나중에 실제 키로 대체하면 됨

                ContestImg img = ContestImg.of(contest, imgUrl, imgKey, baseOrder + i);
                contest.addContestImg(img);
            }
        }
    }


    private static void extractedVideo(ContestRecordCreateDTO.Command contestRecordCommand, Contest contest) {
        if (contestRecordCommand.contestVideos() != null) {
            for (int i = 0; i < contestRecordCommand.contestVideos().size(); i++) {
                String videoUrl = contestRecordCommand.contestVideos().get(i);

                ContestVideo contestVideo = ContestVideo.of(contest, videoUrl, i);

                contest.getContestVideos().add(contestVideo);
            }
        }
    }

    private void extractedImg(ContestRecordCreateDTO.Command contestRecordCommand, Contest contest) {
        if (contestRecordCommand.contestImgs() != null) {
            List<String> imageUrls = imageService.uploadImages(contestRecordCommand.contestImgs());

            for (int i = 0; i < imageUrls.size(); i++) {
                String imgUrl = imageUrls.get(i);
                String uniqueKey = UUID.randomUUID().toString(); // 나중에 파일명 대체 가능

                ContestImg contestImg = ContestImg.of(contest, imgUrl, uniqueKey, i);
                contest.addContestImg(contestImg);
            }
        }
    }

    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new ContestException(ContestErrorCode.MEMBER_NOT_FOUND));
    }
}

