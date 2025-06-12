package org.jeecg.log.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.jeecg.log.dao.UserLogRepository;
import org.jeecg.log.entity.UserLog;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class LogConsumer {

    @Resource
    private UserLogRepository userLogRepository;

    @RabbitListener(queues = "logQueue")
    public void receiveLog(String logMessage) {
        // logMessage 是从 RabbitMQ 中传递过来的
        // 假设日志信息是以 JSON 格式传递的
        // 可以使用 JSON 解析工具，例如 JackSon 来解析这个消息
        try{
            // 假设 LogMessage 是一个 JSON 字符串，如 { "username": "admin", "action": "Login"}
            // logMessage得是 JSON 格式
            // objectMapper.readValue() 方法会将传入的 JSON 字符串 logMessage 转换成 UserLog 实体类的对象。
            // ObjectMapper 只能解析 JSON 字符串 注意是 JSON字符串   {"username":"admin","action":"login"}
            ObjectMapper objectMapper = new ObjectMapper();
            UserLog log = objectMapper.readValue(logMessage, UserLog.class);
            // log.setTimestamp(LocalDateTime.now());

            // 保存日志到 MySQL 数据库
            userLogRepository.save(log);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
