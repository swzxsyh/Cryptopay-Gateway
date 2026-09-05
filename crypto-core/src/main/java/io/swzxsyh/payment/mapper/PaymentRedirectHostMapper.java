package io.swzxsyh.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swzxsyh.payment.persistence.entity.PaymentRedirectHost;
import org.apache.ibatis.annotations.Mapper;

/** 跳转域名白名单访问层。 */
@Mapper
public interface PaymentRedirectHostMapper extends BaseMapper<PaymentRedirectHost> {}
