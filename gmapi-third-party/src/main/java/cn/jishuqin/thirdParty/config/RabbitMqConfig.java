package cn.jishuqin.thirdParty.config;


import cn.jishuqin.common.constant.RabbitMqConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ配置
 * @author gumeng
 */
@Configuration
public class RabbitMqConfig {

    /**
     * 普通队列，订单支付成功队列
     * @return
     */
    @Bean
    public Queue orderPaySuccess(){
        return new Queue(RabbitMqConstant.order_pay_success, true, false, false);
    }

    /**
     * 交换机和队列绑定
     * @return
     */
    @Bean
    public Binding orderPaySuccessBinding(){
        return new Binding(RabbitMqConstant.order_pay_success, Binding.DestinationType.QUEUE,RabbitMqConstant.order_exchange,"order.pay.success",null);
    }

    /**
     * 普通队列
     * @return
     */
    @Bean
    public Queue smsQueue(){
        Map<String, Object> arguments = new HashMap<>();
        //声明死信队列和交换机
        arguments.put("x-dead-letter-exchange", RabbitMqConstant.sms_exchange);
        arguments.put("x-dead-letter-routing-key", "sms.release");
        arguments.put("x-message-ttl", 60000); // 消息过期时间：1分钟
        return new Queue(RabbitMqConstant.sms_queue,true,false,false ,arguments);
    }

    /**
     * 死信队列：消息重试三次后放入死信队列
     * @return
     */
    @Bean
    public Queue deadLetter(){
        return new Queue(RabbitMqConstant.sms_delay_queue, true, false, false);
    }

    /**
     * 交换机
     * @return
     */
    @Bean
    public Exchange smsExchange() {
        return new TopicExchange(RabbitMqConstant.sms_exchange, true, false);
    }

    /**
     * 交换机和普通队列绑定
     * @return
     */
    @Bean
    public Binding smsBinding(){
        return new Binding(RabbitMqConstant.sms_queue, Binding.DestinationType.QUEUE,RabbitMqConstant.sms_exchange,RabbitMqConstant.sms_routingKey,null);
    }

    /**
     * 交换机和死信队列绑定
     * @return
     */
    @Bean
    public Binding smsDelayBinding(){
        return new Binding(RabbitMqConstant.sms_delay_queue, Binding.DestinationType.QUEUE,RabbitMqConstant.sms_exchange,"sms.release",null);
    }
}
