package io.swzxsyh.manager.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swzxsyh.manager.api.dto.ManagerPageResponse;
import io.swzxsyh.manager.api.dto.ManagerRiskDtos.AddressRuleRequest;
import io.swzxsyh.manager.api.dto.ManagerRiskDtos.ChainRuleRequest;
import io.swzxsyh.manager.api.dto.ManagerRiskDtos.TokenRuleRequest;
import io.swzxsyh.payment.mapper.PaymentKytAddressRuleMapper;
import io.swzxsyh.payment.mapper.PaymentKytChainRuleMapper;
import io.swzxsyh.payment.mapper.PaymentKytTokenRuleMapper;
import io.swzxsyh.payment.persistence.entity.PaymentKytAddressRule;
import io.swzxsyh.payment.persistence.entity.PaymentKytChainRule;
import io.swzxsyh.payment.persistence.entity.PaymentKytTokenRule;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ManagerRiskApplicationService extends ManagerApplicationSupport {

  private final PaymentKytAddressRuleMapper addressRuleMapper;
  private final PaymentKytChainRuleMapper chainRuleMapper;
  private final PaymentKytTokenRuleMapper tokenRuleMapper;

  public ManagerRiskApplicationService(
      PaymentKytAddressRuleMapper addressRuleMapper,
      PaymentKytChainRuleMapper chainRuleMapper,
      PaymentKytTokenRuleMapper tokenRuleMapper) {
    this.addressRuleMapper = addressRuleMapper;
    this.chainRuleMapper = chainRuleMapper;
    this.tokenRuleMapper = tokenRuleMapper;
  }

  /** 分页查询 KYT 地址黑白名单规则。 */
  public ManagerPageResponse<PaymentKytAddressRule> pageAddressRules(
      long page, long size, String address, String ruleType, Boolean enabled) {
    return page(addressRuleMapper.selectPage(
        new Page<>(normalizePage(page), normalizeSize(size)),
        Wrappers.<PaymentKytAddressRule>lambdaQuery()
            .like(StringUtils.hasText(address), PaymentKytAddressRule::getAddress, address)
            .eq(StringUtils.hasText(ruleType), PaymentKytAddressRule::getRuleType, normalizeFilterCode(ruleType))
            .eq(enabled != null, PaymentKytAddressRule::getEnabled, enabled)
            .orderByDesc(PaymentKytAddressRule::getUpdatedAt)));
  }

  /** 分页查询 KYT 链维度风控规则。 */
  public ManagerPageResponse<PaymentKytChainRule> pageChainRules(
      long page, long size, String chainCode, String ruleType, Boolean enabled) {
    return page(chainRuleMapper.selectPage(
        new Page<>(normalizePage(page), normalizeSize(size)),
        Wrappers.<PaymentKytChainRule>lambdaQuery()
            .eq(StringUtils.hasText(chainCode), PaymentKytChainRule::getChainCode, normalizeFilterCode(chainCode))
            .eq(StringUtils.hasText(ruleType), PaymentKytChainRule::getRuleType, normalizeFilterCode(ruleType))
            .eq(enabled != null, PaymentKytChainRule::getEnabled, enabled)
            .orderByDesc(PaymentKytChainRule::getUpdatedAt)));
  }

  /** 分页查询 KYT Token 维度风控规则。 */
  public ManagerPageResponse<PaymentKytTokenRule> pageTokenRules(
      long page, long size, String chainCode, String tokenSymbol, String ruleType, Boolean enabled) {
    return page(tokenRuleMapper.selectPage(
        new Page<>(normalizePage(page), normalizeSize(size)),
        Wrappers.<PaymentKytTokenRule>lambdaQuery()
            .eq(StringUtils.hasText(chainCode), PaymentKytTokenRule::getChainCode, normalizeFilterCode(chainCode))
            .eq(StringUtils.hasText(tokenSymbol), PaymentKytTokenRule::getTokenSymbol, normalizeFilterCode(tokenSymbol))
            .eq(StringUtils.hasText(ruleType), PaymentKytTokenRule::getRuleType, normalizeFilterCode(ruleType))
            .eq(enabled != null, PaymentKytTokenRule::getEnabled, enabled)
            .orderByDesc(PaymentKytTokenRule::getUpdatedAt)));
  }

  /** 新增或更新 KYT 地址规则。 */
  @Transactional
  public PaymentKytAddressRule saveAddressRule(AddressRuleRequest request) {
    if (request == null || !StringUtils.hasText(request.address())) {
      throw new IllegalArgumentException("address is required");
    }
    PaymentKytAddressRule entity = new PaymentKytAddressRule();
    entity.setId(request.id());
    entity.setAddress(trimTextToNull(request.address()));
    entity.setRuleType(normalizeFilterCode(request.ruleType()));
    entity.setEnabled(request.enabled());
    touchAndSave(entity.getId(), entity::setCreatedAt, entity::setUpdatedAt,
        () -> addressRuleMapper.insert(entity), () -> addressRuleMapper.updateById(entity));
    return entity;
  }

  /** 新增或更新 KYT 链规则。 */
  @Transactional
  public PaymentKytChainRule saveChainRule(ChainRuleRequest request) {
    if (request == null || !StringUtils.hasText(request.chainCode())) {
      throw new IllegalArgumentException("chainCode is required");
    }
    PaymentKytChainRule entity = new PaymentKytChainRule();
    entity.setId(request.id());
    entity.setChainCode(normalizeFilterCode(request.chainCode()));
    entity.setRuleType(normalizeFilterCode(request.ruleType()));
    entity.setEnabled(request.enabled());
    touchAndSave(entity.getId(), entity::setCreatedAt, entity::setUpdatedAt,
        () -> chainRuleMapper.insert(entity), () -> chainRuleMapper.updateById(entity));
    return entity;
  }

  /** 新增或更新 KYT Token 规则。 */
  @Transactional
  public PaymentKytTokenRule saveTokenRule(TokenRuleRequest request) {
    if (request == null || !StringUtils.hasText(request.tokenSymbol())) {
      throw new IllegalArgumentException("tokenSymbol is required");
    }
    PaymentKytTokenRule entity = new PaymentKytTokenRule();
    entity.setId(request.id());
    entity.setChainCode(normalizeFilterCode(request.chainCode()));
    entity.setTokenSymbol(normalizeFilterCode(request.tokenSymbol()));
    entity.setRuleType(normalizeFilterCode(request.ruleType()));
    entity.setEnabled(request.enabled());
    touchAndSave(entity.getId(), entity::setCreatedAt, entity::setUpdatedAt,
        () -> tokenRuleMapper.insert(entity), () -> tokenRuleMapper.updateById(entity));
    return entity;
  }

  /** 删除 KYT 地址规则。 */
  public boolean deleteAddressRule(Long id) {
    return addressRuleMapper.deleteById(id) > 0;
  }

  /** 删除 KYT 链规则。 */
  public boolean deleteChainRule(Long id) {
    return chainRuleMapper.deleteById(id) > 0;
  }

  /** 删除 KYT Token 规则。 */
  public boolean deleteTokenRule(Long id) {
    return tokenRuleMapper.deleteById(id) > 0;
  }

}
