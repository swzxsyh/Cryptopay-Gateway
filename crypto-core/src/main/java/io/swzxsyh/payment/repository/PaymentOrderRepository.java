package io.swzxsyh.payment.repository;

import io.swzxsyh.payment.domain.PaymentOrder;
import io.swzxsyh.payment.domain.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PaymentOrderRepository {

  PaymentOrder save(PaymentOrder order);

  boolean saveIfStatusIn(PaymentOrder order, Set<OrderStatus> allowedStatuses);

  boolean savePaymentResultIfClaimable(
      PaymentOrder order, Set<OrderStatus> allowedStatuses, String paymentTxHash);

  Optional<PaymentOrder> findByCryptoOrderNo(String cryptoOrderNo);

  Optional<PaymentOrder> findByMerchantIdAndMerchantOrderNo(String merchantId, String merchantOrderNo);

  Optional<PaymentOrder> findByPaymentTxHash(String paymentTxHash);

  List<PaymentOrder> findAll();

  List<PaymentOrder> findActiveByChain(String chain);

  List<PaymentOrder> findExpirableOrders(LocalDateTime now, int limit);

  List<PaymentOrder> findByChainAndDestinationAddress(String chain, String destinationAddress);

  List<PaymentOrder> findByChainAndDestinationAddressAndTokenAddress(
      String chain, String destinationAddress, String tokenAddress);

  Set<String> findActiveDestinationAddresses(String chain, String tokenAddress);

  List<PaymentOrder> findByDestinationAddress(String destinationAddress);

  List<PaymentOrder> findFinishedDerivedAddressLeases();
}
