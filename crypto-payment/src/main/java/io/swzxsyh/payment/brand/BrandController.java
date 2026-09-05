package io.swzxsyh.payment.brand;

import io.swzxsyh.payment.api.dto.ApiResponse;
import io.swzxsyh.payment.vo.BrandVo;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/crypto/brand")
public class BrandController {

  @Value("${crypto.brand.name}")
  private String brandName;

  @GetMapping
  public ApiResponse<BrandVo> brand() {
    BrandVo result = new BrandVo(brandName, null);
    return ApiResponse.ok(result);
  }
}
