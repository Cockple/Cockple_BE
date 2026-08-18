package umc.cockple.demo.domain.game.service.command;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.game.domain.GameBoard;
import umc.cockple.demo.domain.game.domain.GameBoardMember;
import umc.cockple.demo.domain.game.events.GameBoardMembersChangedEvent;
import umc.cockple.demo.domain.game.repository.GameBoardMemberRepository;
import umc.cockple.demo.domain.game.service.command.model.GameBoardMemberCreateCommand;
import umc.cockple.demo.domain.game.service.support.reader.GameBoardReader;
import umc.cockple.demo.domain.game.service.support.validator.GameBoardAccessValidator;

@Service
@Transactional
@RequiredArgsConstructor
public class GameBoardMemberCommandService {

    private final GameBoardReader gameBoardReader;
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

        eventPublisher.publishEvent(new GameBoardMembersChangedEvent(gameBoard.getId(), memberId));
        return savedMember.getId();
    }
}
