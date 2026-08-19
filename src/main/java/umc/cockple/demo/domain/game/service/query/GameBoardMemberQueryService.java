package umc.cockple.demo.domain.game.service.query;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.file.service.ImageUrlResolver;
import umc.cockple.demo.domain.game.domain.Game;
import umc.cockple.demo.domain.game.domain.GameBoardMember;
import umc.cockple.demo.domain.game.domain.GamePlayer;
import umc.cockple.demo.domain.game.enums.GameStatus;
import umc.cockple.demo.domain.game.service.query.model.GameBoardMemberSearchQuery;
import umc.cockple.demo.domain.game.service.query.result.GameBoardMemberResult;
import umc.cockple.demo.domain.game.service.support.reader.GameBoardMemberReader;
import umc.cockple.demo.domain.game.service.support.reader.GameBoardReader;
import umc.cockple.demo.domain.game.service.support.reader.GameReader;
import umc.cockple.demo.domain.game.service.support.validator.GameBoardAccessValidator;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.ProfileImg;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GameBoardMemberQueryService {

    private static final List<GameStatus> ACTIVE_STATUSES = List.of(GameStatus.PLAYING, GameStatus.WAITING);

    private final GameBoardReader gameBoardReader;
    private final GameBoardMemberReader gameBoardMemberReader;
    private final GameReader gameReader;
    private final GameBoardAccessValidator gameBoardAccessValidator;
    private final ImageUrlResolver imageUrlResolver;

    public GameBoardMemberResult getMembers(
            Long memberId, Long gameBoardId, GameBoardMemberSearchQuery searchQuery) {
        gameBoardReader.read(gameBoardId);
        gameBoardAccessValidator.validateViewer(gameBoardId, memberId);

        return loadMembers(gameBoardId, searchQuery);
    }

    public GameBoardMemberResult getMembersSnapshot(
            Long gameBoardId, GameBoardMemberSearchQuery searchQuery) {
        gameBoardReader.read(gameBoardId);

        return loadMembers(gameBoardId, searchQuery);
    }

    private GameBoardMemberResult loadMembers(
            Long gameBoardId, GameBoardMemberSearchQuery searchQuery) {
        int totalCount = Math.toIntExact(gameBoardMemberReader.countByGameBoard(gameBoardId));
        List<GameBoardMember> members = gameBoardMemberReader.readAllByFilters(
                gameBoardId,
                searchQuery.levels(),
                searchQuery.gender(),
                searchQuery.shuttlecockSubmitted());
        Map<Long, EnumSet<GameStatus>> activeStatusesByMemberId = loadActiveStatuses(gameBoardId);

        List<GameBoardMemberResult.MemberView> memberViews = members.stream()
                .map(member -> toView(member, activeStatusesByMemberId.get(member.getId())))
                .toList();

        return new GameBoardMemberResult(totalCount, memberViews);
    }

    private Map<Long, EnumSet<GameStatus>> loadActiveStatuses(Long gameBoardId) {
        List<Game> activeGames = gameReader.readAllByGameBoardAndStatuses(
                gameBoardId, ACTIVE_STATUSES);
        Map<Long, EnumSet<GameStatus>> statusesByMemberId = new HashMap<>();

        for (Game game : activeGames) {
            for (GamePlayer player : game.getPlayers()) {
                statusesByMemberId
                        .computeIfAbsent(player.getGameBoardMember().getId(), ignored -> EnumSet.noneOf(GameStatus.class))
                        .add(game.getStatus());
            }
        }
        return statusesByMemberId;
    }

    private GameBoardMemberResult.MemberView toView(
            GameBoardMember gameBoardMember, EnumSet<GameStatus> activeStatuses) {
        Member sourceMember = gameBoardMember.getMember();
        String profileImageUrl = sourceMember == null
                ? null
                : imageUrlResolver.resolve(sourceMember.getProfileImg(), ProfileImg::getImgKey);

        return new GameBoardMemberResult.MemberView(
                gameBoardMember.getId(),
                hasStatus(activeStatuses, GameStatus.PLAYING),
                hasStatus(activeStatuses, GameStatus.WAITING),
                gameBoardMember.getParticipating(),
                gameBoardMember.getGameCount(),
                profileImageUrl,
                gameBoardMember.getName(),
                gameBoardMember.getGender(),
                gameBoardMember.getAgeGroup(),
                gameBoardMember.getLevel(),
                gameBoardMember.getShuttlecockSubmitted());
    }

    private boolean hasStatus(EnumSet<GameStatus> statuses, GameStatus status) {
        return statuses != null && statuses.contains(status);
    }
}
