package io.swzxsyh.manager.message.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 管理端消息模板，后续短信、邮件、站内信可共用。 */
@Data
@TableName("manager_message_template")
public class ManagerMessageTemplate {

  @TableId(value = "id", type = IdType.AUTO)
  private Long id;

  /** 模板编码，如 APPROVAL_PENDING。 */
  private String templateCode;

  /** 消息通道：EMAIL/PHONE/IN_APP。 */
  private String channel;

  private String templateName;

  private String subject;

  /** 模板正文，变量格式先约定为 ${name}，具体渲染实现后续可替换。 */
  private String content;

  private Boolean enabled;

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;
}
