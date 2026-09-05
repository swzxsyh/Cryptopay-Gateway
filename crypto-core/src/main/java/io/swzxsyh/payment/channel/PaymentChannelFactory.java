package io.swzxsyh.payment.channel;

import io.swzxsyh.payment.domain.PaymentMethod;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class PaymentChannelFactory {

  private final Map<PaymentMethod, List<PaymentChannel>> channels;

  public PaymentChannelFactory(List<PaymentChannel> channelList) {
    this.channels =
        channelList.stream().collect(Collectors.groupingBy(PaymentChannel::method));
  }

  /**
   * 根据支付方式、链和币种选择通道实现。
   *
   * <p>同一种支付方式后续可以有多个链族实现，例如 EVM 派生地址、Solana 派生地址、
   * TRON 派生地址。订单主流程只依赖这个工厂，不需要感知具体链实现。
   */
  public PaymentChannel get(PaymentMethod method, String chain, String token) {
    List<PaymentChannel> candidates = channels.getOrDefault(method, List.of());
    return candidates.stream()
        .filter(channel -> channel.supports(chain, token))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Unsupported payment channel. method="
                        + method
                        + ", chain="
                        + chain
                        + ", token="
                        + token));
  }

  /** 兼容旧调用：仅按支付方式取第一个通道。新代码应使用带 chain/token 的重载。 */
  public PaymentChannel get(PaymentMethod method) {
    List<PaymentChannel> candidates = new ArrayList<>(channels.getOrDefault(method, List.of()));
    if (candidates.isEmpty()) {
      throw new IllegalArgumentException("Unsupported payment method: " + method);
    }
    return candidates.get(0);
  }
}
