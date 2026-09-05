package io.swzxsyh.manager.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swzxsyh.manager.api.dto.ManagerPageResponse;
import io.swzxsyh.manager.api.dto.ManagerRawChainLogHandleRequest;
import io.swzxsyh.payment.mapper.RawChainLogMapper;
import io.swzxsyh.payment.persistence.entity.RawChainLog;
import io.swzxsyh.payment.rawchain.RawChainLogService;
import org.springframework.stereotype.Service;

/** 管理端原始链上流水查询与人工标记服务。 */
@Service
public class ManagerRawChainLogApplicationService extends ManagerApplicationSupport {

  private final RawChainLogMapper rawChainLogMapper;
  private final RawChainLogService rawChainLogService;

  public ManagerRawChainLogApplicationService(
      RawChainLogMapper rawChainLogMapper, RawChainLogService rawChainLogService) {
    this.rawChainLogMapper = rawChainLogMapper;
    this.rawChainLogService = rawChainLogService;
  }

  /** 分页查询原始链上流水，供运营按 txHash、地址或状态排查未匹配入账。 */
  public ManagerPageResponse<RawChainLog> pageRawChainLogs(
      long page, long size, String chain, String txHash, String toAddress, String status) {
    LambdaQueryWrapper<RawChainLog> query =
        Wrappers.<RawChainLog>lambdaQuery()
            .eq(hasText(chain), RawChainLog::getChain, normalizeFilterCode(chain))
            .eq(hasText(txHash), RawChainLog::getTxHash, txHash)
            .eq(hasText(toAddress), RawChainLog::getToAddress, toAddress)
            .eq(hasText(status), RawChainLog::getStatus, normalizeFilterCode(status))
            .orderByDesc(RawChainLog::getBlockNumber)
            .orderByDesc(RawChainLog::getCreatedAt);
    return page(rawChainLogMapper.selectPage(new Page<>(normalizePage(page), normalizeSize(size)), query));
  }

  /** 人工标记原始流水已处理；这里只改变流水处理状态，不自动推进订单。 */
  public RawChainLog markRawChainLogManualProcessed(Long id, ManagerRawChainLogHandleRequest request) {
    return rawChainLogService.markManualProcessed(
        id,
        request == null ? null : request.operator(),
        request == null ? null : request.operatorNote());
  }
}
