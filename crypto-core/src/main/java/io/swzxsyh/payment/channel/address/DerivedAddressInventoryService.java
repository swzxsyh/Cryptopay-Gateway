package io.swzxsyh.payment.channel.address;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.swzxsyh.payment.kyt.KytDecision;
import io.swzxsyh.payment.mapper.DerivedAddressPoolRecordMapper;
import io.swzxsyh.payment.persistence.entity.DerivedAddressPoolRecord;
import io.swzxsyh.payment.persistence.entity.DerivedAddressPoolStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 派生地址库存服务，负责落库、状态迁移和风控标记。 */
@Slf4j
@Service
public class DerivedAddressInventoryService {

  private final DerivedAddressPoolRecordMapper mapper;

  public DerivedAddressInventoryService(DerivedAddressPoolRecordMapper mapper) {
    this.mapper = mapper;
  }

  public long countAvailable(String poolKey) {
    return mapper.selectCount(Wrappers.<DerivedAddressPoolRecord>lambdaQuery()
        .eq(DerivedAddressPoolRecord::getPoolKey, poolKey)
        .eq(DerivedAddressPoolRecord::getStatus, DerivedAddressPoolStatus.AVAILABLE.name())
        .eq(DerivedAddressPoolRecord::isRiskFlag, false));
  }

  public List<DerivedAddressPoolRecord> findAvailable(String poolKey, int limit) {
    List<DerivedAddressPoolRecord> records = mapper.selectList(Wrappers.<DerivedAddressPoolRecord>lambdaQuery()
        .eq(DerivedAddressPoolRecord::getPoolKey, poolKey)
        .eq(DerivedAddressPoolRecord::getStatus, DerivedAddressPoolStatus.AVAILABLE.name())
        .eq(DerivedAddressPoolRecord::isRiskFlag, false)
        .orderByAsc(DerivedAddressPoolRecord::getCreatedAt));
    if (limit > 0 && records.size() > limit) {
      return records.subList(0, limit);
    }
    return records;
  }

  public List<DerivedAddressPoolRecord> findReusableCooldownExpired(String poolKey, int limit) {
    List<DerivedAddressPoolRecord> records = mapper.selectList(Wrappers.<DerivedAddressPoolRecord>lambdaQuery()
        .eq(DerivedAddressPoolRecord::getPoolKey, poolKey)
        .eq(DerivedAddressPoolRecord::getStatus, DerivedAddressPoolStatus.COOLDOWN.name())
        .eq(DerivedAddressPoolRecord::isRiskFlag, false)
        .le(DerivedAddressPoolRecord::getCooldownUntil, LocalDateTime.now())
        .orderByAsc(DerivedAddressPoolRecord::getCooldownUntil)
        .orderByAsc(DerivedAddressPoolRecord::getCreatedAt));
    if (limit > 0 && records.size() > limit) {
      return records.subList(0, limit);
    }
    return records;
  }

  public DerivedAddressPoolRecord markAvailableAfterCooldown(String poolKey, String address) {
    DerivedAddressPoolRecord record = findByPoolKeyAndAddress(poolKey, address)
        .orElseThrow(() -> new IllegalStateException("derived address record not found: " + address));
    if (record.isRiskFlag() || DerivedAddressPoolStatus.RISK_BLOCKED.name().equals(record.getStatus())) {
      return record;
    }
    if (!DerivedAddressPoolStatus.COOLDOWN.name().equals(record.getStatus())) {
      return record;
    }
    record.setStatus(DerivedAddressPoolStatus.AVAILABLE.name());
    record.setCooldownUntil(null);
    record.setUpdatedAt(LocalDateTime.now());
    mapper.updateById(record);
    return record;
  }

  /** 查询可用于归集传递的地址记录。仅返回未被风控且处于可用态的地址。 */
  public List<DerivedAddressPoolRecord> findCollectable(String poolKey, int limit) {
    List<DerivedAddressPoolRecord> records = mapper.selectList(Wrappers.<DerivedAddressPoolRecord>lambdaQuery()
        .eq(DerivedAddressPoolRecord::getPoolKey, poolKey)
        .eq(DerivedAddressPoolRecord::getStatus, DerivedAddressPoolStatus.AVAILABLE.name())
        .eq(DerivedAddressPoolRecord::isRiskFlag, false)
        .orderByAsc(DerivedAddressPoolRecord::getGeneratedAt)
        .orderByAsc(DerivedAddressPoolRecord::getCreatedAt));
    if (limit > 0 && records.size() > limit) {
      return records.subList(0, limit);
    }
    return records;
  }

  /** 查询可用于归集传递的地址字符串列表。 */
  public List<String> findCollectableAddresses(String poolKey, int limit) {
    return findCollectable(poolKey, limit).stream()
        .map(DerivedAddressPoolRecord::getAddress)
        .toList();
  }

  public Optional<DerivedAddressPoolRecord> findByPoolKeyAndAddress(String poolKey, String address) {
    if (!StringUtils.hasText(poolKey) || !StringUtils.hasText(address)) {
      return Optional.empty();
    }
    return Optional.ofNullable(mapper.selectOne(Wrappers.<DerivedAddressPoolRecord>lambdaQuery()
        .eq(DerivedAddressPoolRecord::getPoolKey, poolKey)
        .eq(DerivedAddressPoolRecord::getAddress, address)));
  }

  public DerivedAddressPoolRecord saveGeneratedAddress(
      String poolKey,
      String chain,
      String token,
      String address,
      String sourceMode,
      String sourceReference) {
    DerivedAddressPoolRecord record = findByPoolKeyAndAddress(poolKey, address).orElseGet(DerivedAddressPoolRecord::new);
    if (record.getId() == null) {
      record.setPoolKey(poolKey);
      record.setChain(chain);
      record.setToken(token);
      record.setAddress(address);
      record.setSourceMode(sourceMode);
      record.setSourceReference(sourceReference);
      record.setStatus(DerivedAddressPoolStatus.AVAILABLE.name());
      record.setRiskFlag(false);
      record.setGeneratedAt(LocalDateTime.now());
      record.setCreatedAt(record.getGeneratedAt());
    } else if (record.isRiskFlag() || DerivedAddressPoolStatus.RISK_BLOCKED.name().equals(record.getStatus())) {
      log.warn("Skip saving generated address because it is risk blocked. poolKey={}, address={}", poolKey, address);
      return record;
    } else if (DerivedAddressPoolStatus.LEASED.name().equals(record.getStatus())) {
      log.debug("Skip resetting leased derived address during generation. poolKey={}, address={}", poolKey, address);
      return record;
    } else if (DerivedAddressPoolStatus.RETIRED.name().equals(record.getStatus())) {
      log.debug("Skip re-activating retired derived address during generation. poolKey={}, address={}", poolKey, address);
      return record;
    } else {
      record.setStatus(DerivedAddressPoolStatus.AVAILABLE.name());
      if (!StringUtils.hasText(record.getSourceMode())) {
        record.setSourceMode(sourceMode);
      }
      if (!StringUtils.hasText(record.getSourceReference())) {
        record.setSourceReference(sourceReference);
      }
      if (record.getGeneratedAt() == null) {
        record.setGeneratedAt(LocalDateTime.now());
      }
    }
    record.setUpdatedAt(LocalDateTime.now());
    if (record.getId() == null) {
      mapper.insert(record);
    } else {
      mapper.updateById(record);
    }
    return record;
  }

  public DerivedAddressPoolRecord markLeased(String poolKey, String leaseId, String leaseOrderNo, String address) {
    DerivedAddressPoolRecord record = findByPoolKeyAndAddress(poolKey, address)
        .orElseThrow(() -> new IllegalStateException("derived address record not found: " + address));
    if (record.isRiskFlag() || DerivedAddressPoolStatus.RISK_BLOCKED.name().equals(record.getStatus())) {
      throw new IllegalStateException("derived address is blocked by KYT: " + address);
    }
    if (!DerivedAddressPoolStatus.AVAILABLE.name().equals(record.getStatus())
        && !DerivedAddressPoolStatus.LEASED.name().equals(record.getStatus())) {
      throw new IllegalStateException("derived address is not available: " + address);
    }
    record.setLeaseId(leaseId);
    record.setLeaseOrderNo(leaseOrderNo);
    record.setStatus(DerivedAddressPoolStatus.LEASED.name());
    record.setLeasedAt(LocalDateTime.now());
    record.setUpdatedAt(record.getLeasedAt());
    mapper.updateById(record);
    return record;
  }

  public DerivedAddressPoolRecord markReleased(String poolKey, String leaseId, String reason) {
    return markReleased(poolKey, leaseId, reason, false, 0, null);
  }

  public DerivedAddressPoolRecord markReleased(
      String poolKey,
      String leaseId,
      String reason,
      boolean reuseAllowed,
      int cooldownMinutes,
      String lastTxHash) {
    if (!StringUtils.hasText(poolKey) || !StringUtils.hasText(leaseId)) {
      return null;
    }
    DerivedAddressPoolRecord record = mapper.selectOne(Wrappers.<DerivedAddressPoolRecord>lambdaQuery()
        .eq(DerivedAddressPoolRecord::getPoolKey, poolKey)
        .eq(DerivedAddressPoolRecord::getLeaseId, leaseId));
    if (record == null) {
      return null;
    }
    String lastOrderNo = record.getLeaseOrderNo();
    record.setLeaseId(null);
    record.setLeaseOrderNo(null);
    record.setReleasedAt(LocalDateTime.now());
    record.setUpdatedAt(record.getReleasedAt());
    record.setLastUsedAt(record.getReleasedAt());
    record.setLastOrderNo(lastOrderNo);
    if (StringUtils.hasText(lastTxHash)) {
      record.setLastTxHash(lastTxHash);
    }
    record.setReuseCount((record.getReuseCount() == null ? 0 : record.getReuseCount()) + 1);
    if (record.isRiskFlag() || DerivedAddressPoolStatus.RISK_BLOCKED.name().equals(record.getStatus())) {
      record.setStatus(DerivedAddressPoolStatus.RISK_BLOCKED.name());
    } else if (record.getStatus() != null
        && DerivedAddressPoolStatus.RETIRED.name().equals(record.getStatus())) {
      record.setStatus(DerivedAddressPoolStatus.RETIRED.name());
    } else if (!reuseAllowed) {
      record.setStatus(DerivedAddressPoolStatus.RETIRED.name());
      record.setCooldownUntil(null);
    } else if (cooldownMinutes > 0) {
      record.setStatus(DerivedAddressPoolStatus.COOLDOWN.name());
      record.setCooldownUntil(record.getReleasedAt().plusMinutes(cooldownMinutes));
    } else {
      record.setStatus(DerivedAddressPoolStatus.AVAILABLE.name());
      record.setCooldownUntil(null);
    }
    mapper.updateById(record);
    return record;
  }

  public DerivedAddressPoolRecord markRetired(String poolKey, String address, String reason) {
    if (!StringUtils.hasText(poolKey) || !StringUtils.hasText(address)) {
      return null;
    }
    DerivedAddressPoolRecord record = mapper.selectOne(Wrappers.<DerivedAddressPoolRecord>lambdaQuery()
        .eq(DerivedAddressPoolRecord::getPoolKey, poolKey)
        .eq(DerivedAddressPoolRecord::getAddress, address));
    if (record == null) {
      return null;
    }
    record.setLeaseId(null);
    record.setLeaseOrderNo(null);
    record.setReleasedAt(LocalDateTime.now());
    record.setStatus(DerivedAddressPoolStatus.RETIRED.name());
    record.setUpdatedAt(record.getReleasedAt());
    mapper.updateById(record);
    log.info("Marked derived address retired. poolKey={}, address={}, leaseId={}, reason={}",
        poolKey, record.getAddress(), record.getLeaseId(), reason);
    return record;
  }

  /** 人工恢复地址为可用态。该动作会清理租约、冷置和风控标记，调用方必须先完成运营复核。 */
  public DerivedAddressPoolRecord markAvailableManually(String poolKey, String address, String reason) {
    if (!StringUtils.hasText(poolKey) || !StringUtils.hasText(address)) {
      return null;
    }
    DerivedAddressPoolRecord record = mapper.selectOne(Wrappers.<DerivedAddressPoolRecord>lambdaQuery()
        .eq(DerivedAddressPoolRecord::getPoolKey, poolKey)
        .eq(DerivedAddressPoolRecord::getAddress, address));
    if (record == null) {
      return null;
    }
    if (DerivedAddressPoolStatus.LEASED.name().equals(record.getStatus())) {
      throw new IllegalStateException("leased derived address cannot be restored directly: " + address);
    }
    record.setLeaseId(null);
    record.setLeaseOrderNo(null);
    record.setCooldownUntil(null);
    record.setRiskFlag(false);
    record.setKytDecision(null);
    record.setKytProvider(null);
    record.setKytRiskScore(null);
    record.setRiskReason(null);
    record.setRiskMarkedAt(null);
    record.setStatus(DerivedAddressPoolStatus.AVAILABLE.name());
    record.setUpdatedAt(LocalDateTime.now());
    mapper.updateById(record);
    log.info("Marked derived address manually available. poolKey={}, address={}, reason={}",
        poolKey, record.getAddress(), reason);
    return record;
  }

  public DerivedAddressPoolRecord markRiskBlocked(
      String poolKey,
      String address,
      KytDecision decision,
      String providerId,
      Integer riskScore,
      String reason) {
    DerivedAddressPoolRecord record = findByPoolKeyAndAddress(poolKey, address)
        .orElseThrow(() -> new IllegalStateException("derived address record not found: " + address));
    record.setRiskFlag(true);
    record.setStatus(DerivedAddressPoolStatus.RISK_BLOCKED.name());
    record.setKytDecision(decision == null ? null : decision.name());
    record.setKytProvider(providerId);
    record.setKytRiskScore(riskScore);
    record.setRiskReason(reason);
    record.setRiskMarkedAt(LocalDateTime.now());
    record.setUpdatedAt(record.getRiskMarkedAt());
    record.setLeaseId(null);
    record.setLeaseOrderNo(null);
    mapper.updateById(record);
    return record;
  }

  public boolean isRiskBlocked(String poolKey, String address) {
    return findByPoolKeyAndAddress(poolKey, address)
        .map(record -> record.isRiskFlag() || DerivedAddressPoolStatus.RISK_BLOCKED.name().equals(record.getStatus()))
        .orElse(false);
  }
}
