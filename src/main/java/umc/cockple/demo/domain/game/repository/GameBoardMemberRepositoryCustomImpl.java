package umc.cockple.demo.domain.game.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import umc.cockple.demo.domain.game.domain.GameBoardMember;
import umc.cockple.demo.domain.game.domain.QGameBoardMember;
import umc.cockple.demo.domain.member.domain.QMember;
import umc.cockple.demo.domain.member.domain.QProfileImg;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class GameBoardMemberRepositoryCustomImpl implements GameBoardMemberRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private final QGameBoardMember gameBoardMember = QGameBoardMember.gameBoardMember;
    private final QMember member = QMember.member;
    private final QProfileImg profileImg = QProfileImg.profileImg;

    @Override
    public List<GameBoardMember> findAllByFilters(
            Long gameBoardId,
            List<Level> levels,
            Gender gender,
            Boolean shuttlecockSubmitted) {
        BooleanBuilder conditions = new BooleanBuilder(gameBoardMember.gameBoard.id.eq(gameBoardId));

        if (levels != null && !levels.isEmpty()) {
            conditions.and(gameBoardMember.level.in(levels));
        }
        if (gender != null) {
            conditions.and(gameBoardMember.gender.eq(gender));
        }
        if (shuttlecockSubmitted != null) {
            conditions.and(gameBoardMember.shuttlecockSubmitted.eq(shuttlecockSubmitted));
        }

        return queryFactory
                .selectFrom(gameBoardMember)
                .leftJoin(gameBoardMember.member, member).fetchJoin()
                .leftJoin(member.profileImg, profileImg).fetchJoin()
                .where(conditions)
                .orderBy(gameBoardMember.id.asc())
                .fetch();
    }
}
