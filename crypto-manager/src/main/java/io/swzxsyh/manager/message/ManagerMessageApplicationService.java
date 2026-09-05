package io.swzxsyh.manager.message;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.swzxsyh.manager.message.entity.ManagerMessageSendRecord;
import io.swzxsyh.manager.message.entity.ManagerMessageTemplate;
import io.swzxsyh.manager.message.mapper.ManagerMessageSendRecordMapper;
import io.swzxsyh.manager.message.mapper.ManagerMessageTemplateMapper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 消息编排服务：读取模板、替换变量、调用 sender，并保存发送轨迹。 */
@Service
public class ManagerMessageApplicationService {

  private static final DateTimeFormatter NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

  private final ManagerMessageTemplateMapper templateMapper;
  private final ManagerMessageSendRecordMapper recordMapper;
  private final ManagerMessageSender sender;

  public ManagerMessageApplicationService(
      ManagerMessageTemplateMapper templateMapper,
      ManagerMessageSendRecordMapper recordMapper,
      ManagerMessageSender sender) {
    this.templateMapper = templateMapper;
    this.recordMapper = recordMapper;
    this.sender = sender;
  }

  /** 根据模板编码和接收人发送消息；模板不存在或禁用时直接跳过。 */
  public void sendByTemplate(String templateCode, String channel, String receiver, Map<String, ?> variables) {
    if (!StringUtils.hasText(templateCode) || !StringUtils.hasText(channel) || !StringUtils.hasText(receiver)) {
      return;
    }
    ManagerMessageTemplate template = templateMapper.selectOne(Wrappers.<ManagerMessageTemplate>lambdaQuery()
        .eq(ManagerMessageTemplate::getTemplateCode, templateCode.trim().toUpperCase())
        .eq(ManagerMessageTemplate::getChannel, channel.trim().toUpperCase())
        .eq(ManagerMessageTemplate::getEnabled, Boolean.TRUE)
        .last("limit 1"));
    if (template == null) {
      return;
    }
    String subject = render(template.getSubject(), variables);
    String content = render(template.getContent(), variables);
    ManagerMessageSendRecord record = new ManagerMessageSendRecord();
    record.setMessageNo("MSG" + LocalDateTime.now().format(NO_TIME) + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase());
    record.setTemplateCode(template.getTemplateCode());
    record.setChannel(template.getChannel());
    record.setReceiver(receiver.trim());
    record.setSubject(subject);
    record.setContent(content);
    record.setStatus("PENDING");
    record.setCreatedAt(LocalDateTime.now());
    record.setUpdatedAt(record.getCreatedAt());
    recordMapper.insert(record);

    ManagerMessageSendResult result = sender.send(new ManagerMessageRequest(templateCode, channel, receiver, variables));
    record.setStatus(result.success() ? "SENT" : "FAILED");
    record.setProvider(result.provider());
    record.setProviderMessageId(result.providerMessageId());
    record.setFailureReason(result.success() ? null : result.message());
    record.setSentAt(result.success() ? LocalDateTime.now() : null);
    record.setUpdatedAt(LocalDateTime.now());
    recordMapper.updateById(record);
  }

  private String render(String template, Map<String, ?> variables) {
    String rendered = template == null ? "" : template;
    if (variables == null || variables.isEmpty()) {
      return rendered;
    }
    for (Map.Entry<String, ?> entry : variables.entrySet()) {
      rendered = rendered.replace("${" + entry.getKey() + "}", String.valueOf(entry.getValue()));
    }
    return rendered;
  }
}
