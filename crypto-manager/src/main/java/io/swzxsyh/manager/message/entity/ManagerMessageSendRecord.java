package io.swzxsyh.manager.message.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 管理端消息发送记录，用于追踪审批提醒等通知是否已交给 provider。 */
@Data
@TableName("manager_message_send_record")
public class ManagerMessageSendRecord {

  @TableId(value = "id", type = IdType.AUTO)
  private Long id;

  private String messageNo;

  private String templateCode;

  private String channel;

  private String receiver;

  private String subject;

  private String content;

  private String status;

  private String provider;

  private String providerMessageId;

  private String failureReason;

  private LocalDateTime sentAt;

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;
}
