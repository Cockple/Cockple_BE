package umc.cockple.demo.domain.game.service.command;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.game.domain.GameBoard;
import umc.cockple.demo.domain.game.domain.GameBoardMember;
import umc.cockple.demo.domain.game.enums.GameStatus;
import umc.cockple.demo.domain.game.events.GameBoardMembersChangedEvent;
import umc.cockple.demo.domain.game.exception.GameErrorCode;
import umc.cockple.demo.domain.game.exception.GameException;
import umc.cockple.demo.domain.game.repository.GameBoardMemberRepository;
import umc.cockple.demo.domain.game.service.command.model.GameBoardMemberCreateCommand;
import umc.cockple.demo.domain.game.service.command.model.GameBoardMemberParticipationCommand;
import umc.cockple.demo.domain.game.service.command.model.GameBoardMemberUpdateCommand;
import umc.cockple.demo.domain.game.service.support.reader.GameBoardMemberReader;
import umc.cockple.demo.domain.game.service.support.reader.GameBoardReader;
import umc.cockple.demo.domain.game.service.support.reader.GameReader;
import umc.cockple.demo.domain.game.service.support.validator.GameBoardAccessValidator;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class GameBoardMemberCommandService {

    private static final List<GameStatus> ACTIVE_STATUSES =
            List.of(GameStatus.PLAYING, GameStatus.WAITING);

    private final GameBoardReader gameBoardReader;
    private final GameBoardMemberReader gameBoardMemberReader;
    private final GameReader gameReader;
    private final GameBoardAccessValidator gameBoardAccessValidator;
    private final GameBoardMemberRepository gameBoardMemberRepository;
    private final ApplicationEventPublisher eventPublisher;

    public Long createMember(Long memberId, GameBoardMemberCreateCommand command) {
        gameBoardAccessValidator.validateGameHost(command.gameBoardId(), memberId);
        GameBoard gameBoard = gameBoardReader.read(command.gameBoardId());

        GameBoardMember gameBoardMember = GameBoardMember.create(
                command.name(), command.gender(), command.level(), command.ageGroup());
        gameBoard.addGameBoardMember(gameBoardMember);
        GameBoardMember savedMember = gameBoardMemberRepository.save(gameBoardMember);

        eventPublisher.publishEvent(GameBoardMembersChangedEvent.membersAndBoard(gameBoard.getId(), memberId));
        return savedMember.getId();
    }

    public void updateMember(Long memberId, GameBoardMemberUpdateCommand command) {
        gameBoardAccessValidator.validateGameHost(command.gameBoardId(), memberId);
        GameBoardMember gameBoardMember = gameBoardMemberReader.read(
                command.gameBoardId(), command.gameBoardMemberId());

        gameBoardMember.updateInfo(
                command.name(), command.gender(), command.level(), command.ageGroup());

        eventPublisher.publishEvent(GameBoardMembersChangedEvent.membersAndBoard(command.gameBoardId(), memberId));
    }

    public void changeParticipation(Long memberId, GameBoardMemberParticipationCommand command) {
        gameBoardReader.readForUpdate(command.gameBoardId());
        gameBoardAccessValidator.validateGameHost(command.gameBoardId(), memberId);
        GameBoardMember gameBoardMember = gameBoardMemberReader.read(
                command.gameBoardId(), command.gameBoardMemberId());

        if (gameBoardMember.getParticipating() == command.participating()) {
            return;
        }
        if (!command.participating()
                && gameReader.existsByGameBoardMemberAndStatuses(
                        command.gameBoardMemberId(), ACTIVE_STATUSES)) {
            throw new GameException(GameErrorCode.ACTIVE_GAME_MEMBER_CANNOT_BE_INACTIVE);
        }

        gameBoardMember.changeParticipation(command.participating());
        eventPublisher.publishEvent(GameBoardMembersChangedEvent.membersAndBoard(command.gameBoardId(), memberId));
    }
}
