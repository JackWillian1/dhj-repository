package org.jeecg.modules.producer;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;


    @Component
    public class LogProducer {

        @Resource
        private RabbitTemplate rabbitTemplate;

        // 向 RabbitMQ 队列发送日志消息
        public void sendLog(String logMessage) {
            rabbitTemplate.convertAndSend("logQueue",logMessage);
        }
    }


