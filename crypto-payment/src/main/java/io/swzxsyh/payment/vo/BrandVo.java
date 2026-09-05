package io.swzxsyh.payment.vo;

public class BrandVo {

  private final String name;
  private final String icon;

  public BrandVo(String name, String icon) {
    this.name = name;
    this.icon = icon;
  }

  public String getName() {
    return name;
  }

  public String getIcon() {
    return icon;
  }

  @Override
  public String toString() {
    return "BrandVo{" +
        "name='" + name + '\'' +
        ", icon='" + icon + '\'' +
        '}';
  }
}
