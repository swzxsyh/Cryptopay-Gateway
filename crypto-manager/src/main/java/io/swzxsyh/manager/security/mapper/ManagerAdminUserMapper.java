package io.swzxsyh.manager.security.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swzxsyh.manager.security.entity.ManagerAdminUser;
import org.apache.ibatis.annotations.Mapper;

/** 管理端管理员账号访问层。 */
@Mapper
public interface ManagerAdminUserMapper extends BaseMapper<ManagerAdminUser> {}
