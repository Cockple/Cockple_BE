package umc.cockple.demo.domain.contest.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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
import umc.cockple.demo.domain.image.service.ImageService;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ContestCommandServiceImpl implements ContestCommandService {

    private final ContestRepository contestRepository;
    private final MemberRepository memberRepository;
    private final ContestConverter contestConverter;
    private final ImageService imageService;



    // 등록
    @Override
    public ContestRecordCreateDTO.Response createContestRecord(
            Long memberId, ContestRecordCreateDTO.Request request) {

        log.info("[대회 기록 등록 시작] - memberId: {}, 대회명: {}", memberId, request.contestName());

        // 1. 회원 조회
        Member member = getMember(memberId);

        //2. DTO -> Command
        ContestRecordCreateDTO.Command command = contestConverter.toCreateCommand(request, memberId);

        // 3. Command → Contest Entity 생성
        Contest contest = Contest.create(command, member);

        // 4. 이미지 → ContestImg로 매핑
        if (command.contestImgs() != null) {
            extractedImgsWithOrder(command.contestImgs(), contest);
        }

        // 5. 영상 URL -> ContestVideo로 변환
        try {
            extractedVideoWithOrder(command.contestVideos(), contest);
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
        return contestConverter.toCreateResponseDTO(savedContest);
    }

    // 수정
    @Override
    public ContestRecordUpdateDTO.Response updateContestRecord(
            Long memberId, Long contestId, ContestRecordUpdateDTO.Request request
    ) {

        log.info("[대회 기록 수정 시작] - memberId: {}, contestId: {}", memberId, contestId);

        Contest contest = contestRepository.findByIdAndMember_Id(contestId, memberId)
                .orElseThrow(() -> new ContestException(ContestErrorCode.CONTEST_NOT_FOUND));

        // 1. 기본 필드 수정
        contest.updateFromRequest(request);

        // 2. 이미지 전체 교체
        updateContestImages(contest, request.contestImgs());

        // 3. 비디오 전체 교체
        try {
            updateContestVideos(contest, request.contestVideos());
        } catch (Exception e) {
            log.error("영상 수정 중 오류 발생", e);
            throw new ContestException(ContestErrorCode.VIDEO_URL_SAVE_FAIL);
        }

        // 4. 저장
        try {
            contestRepository.save(contest);
        } catch (Exception e) {
            log.error("대회 기록 수정 저장 중 예외 발생", e);
            throw new ContestException(ContestErrorCode.CONTEST_SAVE_FAIL);
        }

        log.info("대회 기록 수정 완료 - contestId: {}", contestId);

        return contestConverter.toUpdateResponseDTO(contest);
    }

    private void updateContestImages(Contest contest, List<ContestImgUpdateRequest> requestImgs) {
        if (requestImgs == null) {
            requestImgs = List.of();
        }

        if (requestImgs.size() > 3) {
            log.error("이미지 개수 초과: {}", requestImgs.size());
            throw new ContestException(ContestErrorCode.IMAGE_UPLOAD_LIMIT_EXCEEDED);
        }

        // 요청에 포함된 기존 이미지 ID 목록
        Set<Long> requestImgIds = requestImgs.stream()
                .map(ContestImgUpdateRequest::id)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        // 기존 이미지 Map (id -> entity)
        Map<Long, ContestImg> existingImgMap = contest.getContestImgs().stream()
                .collect(Collectors.toMap(ContestImg::getId, img -> img));

        // 요청에 없는 기존 이미지 삭제
        List<ContestImg> imgsToRemove = contest.getContestImgs().stream()
                .filter(img -> !requestImgIds.contains(img.getId()))
                .toList();

        for (ContestImg img : imgsToRemove) {
            imageService.delete(img.getImgKey());
            contest.getContestImgs().remove(img);
        }

        // 요청 처리: 기존 항목은 순서 업데이트, 신규 항목은 추가
        for (ContestImgUpdateRequest reqImg : requestImgs) {
            if (reqImg.id() != null) {
                // 기존 항목 - 순서 업데이트
                ContestImg existingImg = existingImgMap.get(reqImg.id());
                if (existingImg != null) {
                    existingImg.setImgOrder(reqImg.imgOrder());
                }
            } else {
                // 신규 항목 추가
                ContestImg newImg = ContestImg.of(contest, reqImg.imgKey(), reqImg.imgOrder());
                contest.addContestImg(newImg);
            }
        }
    }

    private void updateContestVideos(Contest contest, List<ContestVideoUpdateRequest> requestVideos) {
        if (requestVideos == null) {
            requestVideos = List.of();
        }

        // 요청에 포함된 기존 비디오 ID 목록
        Set<Long> requestVideoIds = requestVideos.stream()
                .map(ContestVideoUpdateRequest::id)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        // 기존 비디오 Map (id -> entity)
        Map<Long, ContestVideo> existingVideoMap = contest.getContestVideos().stream()
                .collect(Collectors.toMap(ContestVideo::getId, video -> video));

        // 요청에 없는 기존 비디오 삭제
        List<ContestVideo> videosToRemove = contest.getContestVideos().stream()
                .filter(video -> !requestVideoIds.contains(video.getId()))
                .toList();

        for (ContestVideo video : videosToRemove) {
            contest.getContestVideos().remove(video);
        }

        // 요청 처리: 기존 항목은 순서 업데이트, 신규 항목은 추가
        for (ContestVideoUpdateRequest reqVideo : requestVideos) {
            if (reqVideo.id() != null) {
                // 기존 항목 - 순서 업데이트
                ContestVideo existingVideo = existingVideoMap.get(reqVideo.id());
                if (existingVideo != null) {
                    existingVideo.setVideoOrder(reqVideo.videoOrder());
                }
            } else {
                // 신규 항목 추가
                ContestVideo newVideo = ContestVideo.of(contest, reqVideo.videoKey(), reqVideo.videoOrder());
                contest.getContestVideos().add(newVideo);
            }
        }
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

    private void extractedImgsWithOrder(List<AddContestImgRequest> imgs, Contest contest) {
        int total = contest.getContestImgs().size() + imgs.size();
        if (total > 3) {
            log.error("이미지 개수 초과: 기존 {}, 추가 {}", contest.getContestImgs().size(), imgs.size());
            throw new ContestException(ContestErrorCode.IMAGE_UPLOAD_LIMIT_EXCEEDED);
        }
        for (AddContestImgRequest img : imgs) {
            ContestImg contestImg = ContestImg.of(contest, img.imgKey(), img.imgOrder());
            contest.addContestImg(contestImg);
        }
    }

    private void extractedVideoWithOrder(List<AddContestVideoRequest> videos, Contest contest) {
        if (videos != null && !videos.isEmpty()) {
            for (AddContestVideoRequest video : videos) {
                ContestVideo contestVideo = ContestVideo.of(contest, video.videoKey(), video.videoOrder());
                contest.getContestVideos().add(contestVideo);
            }
        }
    }

    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new ContestException(ContestErrorCode.MEMBER_NOT_FOUND));
    }
}
