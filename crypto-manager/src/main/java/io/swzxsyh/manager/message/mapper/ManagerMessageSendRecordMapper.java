package io.swzxsyh.manager.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swzxsyh.manager.message.entity.ManagerMessageSendRecord;
import org.apache.ibatis.annotations.Mapper;

/** 管理端消息发送记录 Mapper。 */
@Mapper
public interface ManagerMessageSendRecordMapper extends BaseMapper<ManagerMessageSendRecord> {}
