package org.jeecg.log.config;

// amqp是用于与 RabbitMQ 交互的库
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    /*@Bean 是 Spring 提供的一个注解，用于将一个方法的返回值注册为 Spring 容器中的一个 Bean。
    它通常用在 @Configuration 类中，表示：这个方法产生的对象要交给 Spring 容器管理。*/
    @Bean
    public Queue logQueue() {
        return new Queue("logQueue",true);  // 这个对象将由 Spring 管理
      /*  Queue是 Spring AMQP 提供的类，用于定义一个 消息队列，作用：
        保存消息：它用于存储 RabbitMQ 中传递的消息，直到消费者处理完毕。
        持久化：队列是否在 RabbitMQ 服务重启后依然存在，取决于它是否持久化。*/

      /*  "logQueue"：这是队列的名字，RabbitMQ 中将会创建一个名为 logQueue 的队列。
      消费者会监听这个队列，接收从生产者发送到该队列的消息。
        true：这个参数表示队列是持久化的，也就是说，即使 RabbitMQ 服务重启，队列中的消息依然会被保留。
        这个选项对于保存重要数据非常有用。
        如果设置为 false，那么队列和消息会在 RabbitMQ 重启时丢失。
        */

    }

}
