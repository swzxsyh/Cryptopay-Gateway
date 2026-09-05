package io.swzxsyh.payment.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 派生地址生成策略配置，助记词可通过 ENV/YAML/KMS/JNI 多来源提供。 */
@Data
@TableName("payment_derived_address_config")
public class PaymentDerivedAddressConfig {
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  private String configScope;
  private Boolean derivedEnabled;
  private Boolean reuseAddress;
  private Integer reuseCooldownMinutes;
  private String mode;
  private String hdMnemonicSourceType;
  private String hdMnemonicCiphertext;
  private String hdMnemonicKeyId;
  private String hdMnemonicEnv;
  private String hdPassphraseEnv;
  private String hdDerivationPathPrefix;
  private Integer hdStartIndex;
  private Boolean hdGenerateMnemonicWhenMissing;
  private String keystoreOutputDir;
  private String keystorePasswordEnv;
  private String keystoreFilePrefix;
  private Boolean thirdPartyEnabled;
  private String thirdPartyBaseUrl;
  private String thirdPartyCreatePath;
  private String thirdPartyApiKeyEnv;
  private String thirdPartyProviderName;
  private String addressFactoryNamespace;
  private String addressFactorySeedPrefix;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
