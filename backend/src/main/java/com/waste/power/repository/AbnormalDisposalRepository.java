package com.waste.power.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.waste.power.domain.AbnormalDisposal;

/**
 * 异常工况处置单数据访问。
 *
 * 同炉唯一有效处置单：abnormal_disposal.active_once 是生成列（未关闭时为炉号，关闭后为 NULL），
 * 配合唯一索引在库级兜底；应用层再用 findActiveByFurnaceCode 做显式判断与友好报错。
 * 同炉动作的串行化统一靠焚烧炉行锁（incinerator.lockByFurnaceCode），
 * 这里不对「可能零行」的结果集加 FOR UPDATE，避免 MySQL 间隙锁与并发建单互锁。
 */
public interface AbnormalDisposalRepository extends JpaRepository<AbnormalDisposal, Long> {

    /** 未关闭的同炉处置单（含待处置/处置中/待复核/已恢复，各状态最多一条） */
    @Query("select d from AbnormalDisposal d where d.furnaceCode = :furnaceCode and d.status <> 'CLOSED'")
    Optional<AbnormalDisposal> findActiveByFurnaceCode(@Param("furnaceCode") String furnaceCode);

    List<AbnormalDisposal> findTop50ByOrderByCreateTimeDesc();

    @Query("select d from AbnormalDisposal d where d.status <> 'CLOSED' order by d.createTime desc")
    List<AbnormalDisposal> findActiveOrderByCreateTimeDesc();

    /** 所有处于投料冻结的处置单（含「关闭处置但保持投料冻结」的 CLOSED 单，联锁必须继续生效） */
    @Query("select d from AbnormalDisposal d where d.feedFrozen = true")
    List<AbnormalDisposal> findFrozenActive();

    /** 复核恢复通过后解冻同炉其余冻结单（当前单据走实体更新，不进批量语句，避免与持久化上下文互相覆盖） */
    @Modifying(flushAutomatically = true)
    @Query("update AbnormalDisposal d set d.feedFrozen = false, d.unfrozenTime = :now, d.updateTime = :now "
            + "where d.furnaceCode = :furnaceCode and d.feedFrozen = true and d.id <> :currentId")
    int unfreezeOtherFurnace(@Param("furnaceCode") String furnaceCode,
            @Param("currentId") Long currentId,
            @Param("now") java.time.LocalDateTime now);
}
