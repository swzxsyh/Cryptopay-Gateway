package io.swzxsyh.manager.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swzxsyh.manager.api.dto.ManagerPageResponse;
import io.swzxsyh.manager.api.dto.ManagerScannerCheckpointRequest;
import io.swzxsyh.payment.mapper.ChainScannerCheckpointMapper;
import io.swzxsyh.payment.persistence.entity.ChainScannerCheckpoint;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 管理端链扫描 checkpoint 查询和人工校准服务。 */
@Service
public class ManagerScannerApplicationService extends ManagerApplicationSupport {

  private final ChainScannerCheckpointMapper scannerCheckpointMapper;

  public ManagerScannerApplicationService(ChainScannerCheckpointMapper scannerCheckpointMapper) {
    this.scannerCheckpointMapper = scannerCheckpointMapper;
  }

  /** 分页查询链扫描 checkpoint，用于扫描高度和回补观测页面。 */
  public ManagerPageResponse<ChainScannerCheckpoint> pageScannerCheckpoints(
      long page, long size, String chain) {
    return page(scannerCheckpointMapper.selectPage(
        new Page<>(normalizePage(page), normalizeSize(size)),
        Wrappers.<ChainScannerCheckpoint>lambdaQuery()
            .eq(hasText(chain), ChainScannerCheckpoint::getChain, normalizeFilterCode(chain))
            .orderByAsc(ChainScannerCheckpoint::getChain)));
  }

  /** 人工新增或调整链扫描 checkpoint。 */
  @Transactional
  public ChainScannerCheckpoint saveScannerCheckpoint(ManagerScannerCheckpointRequest request) {
    if (request == null || !hasText(request.chain())) {
      throw new IllegalArgumentException("chain is required");
    }
    ChainScannerCheckpoint entity = new ChainScannerCheckpoint();
    entity.setChain(request.chain());
    entity.setLatestObservedBlock(request.latestObservedBlock() == null ? 0 : request.latestObservedBlock());
    entity.setLastConfirmedBlock(request.lastConfirmedBlock() == null ? 0 : request.lastConfirmedBlock());
    entity.setCreatedAt(request.createdAt());
    LocalDateTime now = LocalDateTime.now();
    ChainScannerCheckpoint existing = scannerCheckpointMapper.selectById(entity.getChain());
    entity.setUpdatedAt(now);
    if (existing == null) {
      entity.setCreatedAt(now);
      scannerCheckpointMapper.insert(entity);
    } else {
      if (entity.getCreatedAt() == null) {
        entity.setCreatedAt(existing.getCreatedAt());
      }
      scannerCheckpointMapper.updateById(entity);
    }
    return entity;
  }
}
