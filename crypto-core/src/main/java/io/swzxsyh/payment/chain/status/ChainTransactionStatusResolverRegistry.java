package io.swzxsyh.payment.chain.status;

import java.util.List;
import org.springframework.stereotype.Service;

/** 链状态解析策略注册表，按链编码路由到对应 resolver。 */
@Service
public class ChainTransactionStatusResolverRegistry {

  private final List<ChainTransactionStatusResolver> resolvers;

  public ChainTransactionStatusResolverRegistry(List<ChainTransactionStatusResolver> resolvers) {
    this.resolvers = resolvers;
  }

  public ChainTransactionStatusResult resolve(ChainTransactionStatusContext context) {
    if (context == null || context.pending() == null) {
      return ChainTransactionStatusResult.unsupported("pending transaction is required");
    }
    return resolvers.stream()
        .filter(resolver -> resolver.supports(context.pending().getChain()))
        .findFirst()
        .map(resolver -> resolver.resolve(context))
        .orElseGet(() -> ChainTransactionStatusResult.unsupported(
            "no chain transaction status resolver for chain: " + context.pending().getChain()));
  }
}
