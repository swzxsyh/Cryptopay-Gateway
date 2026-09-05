package io.swzxsyh.payment.settlement.record;

import io.swzxsyh.payment.accounting.MerchantBalanceLedgerService;
import io.swzxsyh.payment.callback.PaymentCallbackDeliveryService;
import io.swzxsyh.payment.domain.OrderStatus;
import io.swzxsyh.payment.domain.PaymentOrder;
import io.swzxsyh.payment.service.PaymentOrderService;
import io.swzxsyh.watcher.dto.PaymentNotificationRecord;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 合约隔离资金放行后的最终闭环服务：更新结算记录、订单状态、商户余额并通知商户。 */
@Slf4j
@Service
public class ContractSettlementFinalizationService {

  private final ContractSettlementRecordService settlementRecordService;
  private final PaymentOrderService paymentOrderService;
  private final MerchantBalanceLedgerService merchantBalanceLedgerService;
  private final PaymentCallbackDeliveryService callbackDeliveryService;

  public ContractSettlementFinalizationService(
      ContractSettlementRecordService settlementRecordService,
      PaymentOrderService paymentOrderService,
      MerchantBalanceLedgerService merchantBalanceLedgerService,
      PaymentCallbackDeliveryService callbackDeliveryService) {
    this.settlementRecordService = settlementRecordService;
    this.paymentOrderService = paymentOrderService;
    this.merchantBalanceLedgerService = merchantBalanceLedgerService;
    this.callbackDeliveryService = callbackDeliveryService;
  }

  /**
   * 标记合约隔离资金已放行，并在订单首次进入最终支付状态时执行余额入账和商户回调。
   *
   * <p>如果订单此前已经完成，本方法只幂等更新/返回结算记录，不重复入账和回调。
   */
  @Transactional(rollbackFor = Exception.class)
  public ContractSettlementRecordView markReleasedAndFinalize(
      String cryptoOrderNo, String txHash, Long blockNumber) {
    ContractSettlementRecordView settlement =
        settlementRecordService.markReleased(cryptoOrderNo, txHash, blockNumber)
            .orElseThrow(() -> new IllegalArgumentException("settlement not found"));
    Optional<PaymentOrder> finalizedOrder =
        paymentOrderService.finalizeContractPaymentRelease(cryptoOrderNo);
    finalizedOrder.ifPresent(order -> postFinalize(order, settlement));
    return settlement;
  }

  private void postFinalize(PaymentOrder order, ContractSettlementRecordView settlement) {
    merchantBalanceLedgerService.creditPaymentOrder(order);
    enqueuePaymentCallback(order, settlement);
  }

  private void enqueuePaymentCallback(PaymentOrder order, ContractSettlementRecordView settlement) {
    if (!StringUtils.hasText(order.getNotifyUrl())) {
      log.warn("合约订单放行成功但回调地址为空，跳过通知。cryptoOrderNo={}, txHash={}",
          order.getCryptoOrderNo(), order.getPaymentTxHash());
      return;
    }
    PaymentNotificationRecord notification =
        new PaymentNotificationRecord(
            order.getChain(),
            order.getCryptoOrderNo(),
            order.getMerchantId(),
            order.getMerchantOrderNo(),
            order.getPaymentTxHash(),
            order.getWalletAddress(),
            order.getContractAddress(),
            order.getAmount(),
            order.getRealAmount(),
            settlement.blockNumber(),
            order.getTransactionFee(),
            order.getTaxFee(),
            order.getTotalFee(),
            order.getSettlementAmount(),
            order.isLatePayment(),
            order.getStatus().name());
    callbackDeliveryService.enqueueAndDispatch(
        resolvePaymentCallbackEvent(order.getStatus()),
        order.getMerchantId(),
        order.getNotifyUrl(),
        notification);
    log.info("合约订单放行成功回调已入队。cryptoOrderNo={}, merchantId={}, txHash={}, status={}",
        order.getCryptoOrderNo(), order.getMerchantId(), order.getPaymentTxHash(), order.getStatus());
  }

  private String resolvePaymentCallbackEvent(OrderStatus orderStatus) {
    if (orderStatus == OrderStatus.UNDERPAID) {
      return "PAYMENT_UNDERPAID";
    }
    if (orderStatus == OrderStatus.OVERPAID) {
      return "PAYMENT_OVERPAID";
    }
    return "PAYMENT_SUCCESS";
  }
}
