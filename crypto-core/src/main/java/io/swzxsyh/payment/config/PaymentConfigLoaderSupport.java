package io.swzxsyh.payment.config;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import org.springframework.util.StringUtils;

/** 配置加载器的通用小工具，避免各个配置贡献者重复空值判断。 */
abstract class PaymentConfigLoaderSupport {

  protected <T> Optional<T> first(List<T> rows) {
    return rows.stream().filter(Objects::nonNull).findFirst();
  }

  protected <T> Optional<T> firstEnabled(List<T> rows, Function<T, Boolean> enabled) {
    return rows.stream()
        .filter(Objects::nonNull)
        .filter(row -> Boolean.TRUE.equals(enabled.apply(row)))
        .findFirst();
  }

  protected <T> void setIfPresent(T value, Consumer<T> setter) {
    if (value != null) {
      setter.accept(value);
    }
  }

  protected void setIfText(String value, Consumer<String> setter) {
    if (StringUtils.hasText(value)) {
      setter.accept(value);
    }
  }

  protected String normalize(String value) {
    return StringUtils.hasText(value) ? value.trim().toUpperCase() : "";
  }
}
