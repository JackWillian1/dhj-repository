package org.jeecg.log.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// @RestController 表示这是一个 RESTful 控制器，返回的内容默认是 JSON 字符串。
// @RestController = @Controller + @ResponseBody。
// @Controller注解表示这个类是一个控制器类（处理 HTTP 请求的类）。
// @ResponseBody注解表示该控制器方法返回的对象会自动被序列化为 JSON 或 XML 格式，并直接写入 HTTP 响应体中。

@RestController
@RequestMapping("/test")
public class TestController {

    @Resource
    // RabbitTemplate 是 Spring AMQP 提供的工具类，用来发送消息到 RabbitMQ
    // 它封装了连接、交换机、路由键、队列等底层操作，提供了简单的发送消息方法
    private RabbitTemplate rabbitTemplate;

    @PostMapping("/send-log")
    // @RequestBody 注解表示：请求体中的 JSON 数据会自动转换为 Map<String, String> 类型。
    public String sendLog(@RequestBody Map<String, String> body) {
        /*
            {
              "username": "admin",
              "action": "login"
            }
            Spring 会自动把它转换为：
            Map<String, String> body = Map.of("username", "admin", "action", "login");
         */
            try{
                ObjectMapper mapper = new ObjectMapper();
                String logMessage = mapper.writeValueAsString(body);
                /* ObjectMapper.writeValueAsString(body)：把 Map 对象转换为 JSON 字符串，结果类似：
                    {"username":"admin","action":"login"}
                 */
                // rabbitTemplate.convertAndSend("logQueue", logMessage)：
                // 将这个 JSON 字符串作为消息，发送到名为 logQueue 的 RabbitMQ 队列。
                rabbitTemplate.convertAndSend("logQueue", logMessage);
                return "Log sent to queue.";
            }catch (Exception e){
                e.printStackTrace();
                return "Failed to send log.";
            }
    }
}

