package umc.cockple.demo.domain.member.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberParty;
import umc.cockple.demo.domain.member.enums.MemberPartyStatus;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.global.enums.Role;

import java.util.List;
import java.util.Optional;

public interface MemberPartyRepository extends JpaRepository<MemberParty, Long> {

    boolean existsByPartyIdAndMemberIdAndRole(Long partyId, Long memberId, Role role);

    boolean existsByPartyAndMember(Party party, Member member);

    boolean existsByPartyIdAndMemberId(Long partyId, Long memberId);

    @Query("""
            SELECT mp FROM MemberParty mp
            WHERE mp.party.id = :partyId
            AND mp.member.id IN :memberIds
            """)
    List<MemberParty> findMemberRolesByPartyAndMembers(
            @Param("partyId") Long partyId, @Param("memberIds") List<Long> memberIds);

    Optional<MemberParty> findByPartyAndMember(Party party, Member member);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT mp FROM MemberParty mp
            WHERE mp.party.id = :partyId
            AND mp.member.id = :memberId
            """)
    Optional<MemberParty> findByPartyIdAndMemberIdForUpdate(
            @Param("partyId") Long partyId,
            @Param("memberId") Long memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT mp FROM MemberParty mp
            WHERE mp.party.id = :partyId
            AND mp.member.id = :memberId
            AND mp.status = :status
            """)
    Optional<MemberParty> findByPartyIdAndMemberIdAndStatusForUpdate(
            @Param("partyId") Long partyId,
            @Param("memberId") Long memberId,
            @Param("status") MemberPartyStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT mp FROM MemberParty mp
            WHERE mp.member.id = :memberId
            ORDER BY mp.party.id, mp.id
            """)
    List<MemberParty> findAllByMemberIdForUpdate(@Param("memberId") Long memberId);

    Slice<MemberParty> findByMember(Member member, Pageable pageable);

    void deleteAllByMember(Member member);

    @Query("select mp.party.id " +
            "from MemberParty mp " +
            "where mp.member.id = :memberId and mp.party.id in :partyIds")
    List<Long> findAllPartyIdsByMemberAndPartyIds(@Param("memberId") Long memberId,
                                                  @Param("partyIds") List<Long> partyIds);

    @Query("""
            SELECT mp.party.id
            FROM MemberParty mp
            WHERE mp.member.id = :memberId
            """)
    List<Long> findPartyIdsByMemberId(@Param("memberId") Long memberId);

    @Query("""
       SELECT mp FROM MemberParty mp
       JOIN FETCH mp.member
       WHERE mp.party.id = :partyId
       """)
    List<MemberParty> findAllByPartyIdWithMember(@Param("partyId") Long partyId);

    @Query("""
            SELECT mp FROM MemberParty mp
            JOIN FETCH mp.member m
            LEFT JOIN FETCH m.profileImg
            WHERE mp.party.id = :partyId
            AND mp.status = :status
            """)
    List<MemberParty> findAllByPartyIdAndStatusWithMemberAndProfile(
            @Param("partyId") Long partyId,
            @Param("status") MemberPartyStatus status);

    Optional<MemberParty> findByPartyIdAndRole(Long partyId, Role role);

    // 프로필의 가입 모임 수. 컬렉션 전량 로딩 없이 COUNT만 수행한다.
    long countByMember_Id(Long memberId);
}
