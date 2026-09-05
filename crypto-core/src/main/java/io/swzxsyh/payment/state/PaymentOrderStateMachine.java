package io.swzxsyh.payment.state;

import io.swzxsyh.payment.domain.OrderStatus;
import org.springframework.stereotype.Component;

/** 普通订单状态机，保证状态只向前推进。 */
@Component
public class PaymentOrderStateMachine {

  /** 是否允许进入 WAITING_PAYMENT。 */
  public boolean canEnterWaitingPayment(OrderStatus current) {
    return current == null
        || current == OrderStatus.CREATED
        || current == OrderStatus.METHOD_SELECTED
        || current == OrderStatus.WAITING_PAYMENT;
  }

  /** 是否允许进入 CONFIRMING。 */
  public boolean canEnterConfirming(OrderStatus current) {
    return !isSettledOrCancelled(current) && current != OrderStatus.CONFIRMING;
  }

  /** 是否允许标记为已支付。 */
  public boolean canMarkPaid(OrderStatus current) {
    return !isSettledOrCancelled(current);
  }

  /** 是否允许标记为少付。 */
  public boolean canMarkUnderpaid(OrderStatus current) {
    return !isSettledOrCancelled(current);
  }

  /** 是否允许标记为多付。 */
  public boolean canMarkOverpaid(OrderStatus current) {
    return !isSettledOrCancelled(current);
  }

  /** 是否允许记录已提交到链上的交易哈希。 */
  public boolean canEnterDetected(OrderStatus current) {
    return current == OrderStatus.METHOD_SELECTED
        || current == OrderStatus.WAITING_PAYMENT
        || current == OrderStatus.DETECTED;
  }

  /** 是否允许进入 KYT 风控挂起态。 */
  public boolean canEnterKytReview(OrderStatus current) {
    return current != OrderStatus.CANCELLED
        && current != OrderStatus.EXPIRED
        && current != OrderStatus.KYT_REVIEW;
  }

  /** 是否允许标记为过期。 */
  public boolean canExpire(OrderStatus current) {
    return current == OrderStatus.CREATED
        || current == OrderStatus.METHOD_SELECTED
        || current == OrderStatus.WAITING_PAYMENT
        || current == OrderStatus.DETECTED;
  }

  /** 推进到 WAITING_PAYMENT。 */
  public OrderStatus waitingPayment(OrderStatus current) {
    if (!canEnterWaitingPayment(current)) {
      throw new IllegalStateException("Order status does not allow waiting payment: " + current);
    }
    return OrderStatus.WAITING_PAYMENT;
  }

  /** 推进到 CONFIRMING，表示资金已进入隔离阶段，等待后续放行。 */
  public OrderStatus confirming(OrderStatus current) {
    if (!canEnterConfirming(current)) {
      throw new IllegalStateException("Order status does not allow confirming transition: " + current);
    }
    return OrderStatus.CONFIRMING;
  }

  /** 推进到 PAID。 */
  public OrderStatus paid(OrderStatus current) {
    if (!canMarkPaid(current)) {
      throw new IllegalStateException("Order status does not allow paid transition: " + current);
    }
    return OrderStatus.PAID;
  }

  /** 推进到 UNDERPAID，表示真实到账金额小于订单金额。 */
  public OrderStatus underpaid(OrderStatus current) {
    if (!canMarkUnderpaid(current)) {
      throw new IllegalStateException("Order status does not allow underpaid transition: " + current);
    }
    return OrderStatus.UNDERPAID;
  }

  /** 推进到 OVERPAID，表示真实到账金额大于订单金额。 */
  public OrderStatus overpaid(OrderStatus current) {
    if (!canMarkOverpaid(current)) {
      throw new IllegalStateException("Order status does not allow overpaid transition: " + current);
    }
    return OrderStatus.OVERPAID;
  }

  /** 推进到 DETECTED，表示前端或链上观察到交易但还未满足确认数。 */
  public OrderStatus detected(OrderStatus current) {
    if (!canEnterDetected(current)) {
      throw new IllegalStateException("Order status does not allow detected transition: " + current);
    }
    return OrderStatus.DETECTED;
  }

  /** 推进到 KYT_REVIEW。 */
  public OrderStatus kytReview(OrderStatus current) {
    if (!canEnterKytReview(current)) {
      throw new IllegalStateException("Order status does not allow KYT review transition: " + current);
    }
    return OrderStatus.KYT_REVIEW;
  }

  /** 推进到 EXPIRED。 */
  public OrderStatus expired(OrderStatus current) {
    if (!canExpire(current)) {
      throw new IllegalStateException("Order status does not allow expire transition: " + current);
    }
    return OrderStatus.EXPIRED;
  }

  /** 已经有明确资金结果或已取消的订单，不再被新的链上交易覆盖。 */
  public boolean isSettledOrCancelled(OrderStatus current) {
    return current == OrderStatus.PAID
        || current == OrderStatus.UNDERPAID
        || current == OrderStatus.OVERPAID
        || current == OrderStatus.CANCELLED;
  }
}
