package cn.jishuqin.order.utils;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.IdUtil;
import cn.jishuqin.common.constant.RabbitMqConstant;
import cn.jishuqin.common.constant.RedisConstant;
import cn.jishuqin.common.model.entity.ApiOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import static cn.jishuqin.common.constant.RabbitMqConstant.order_exchange;
import static cn.jishuqin.common.constant.RedisConstant.SEND_ORDER_PREFIX;


/**
 * 向mq延时队列中发送订单消息
 * @author gumeng
 */
@Slf4j
@Component
public class RabbitOrderUtils implements RabbitTemplate.ConfirmCallback{


    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private Long finalId = null;

    /**
     * 向mq发送订单消息
     * @param apiOrder
     */
    public void sendOrderSnInfo(ApiOrder apiOrder){
        finalId = apiOrder.getId();
        redisTemplate.opsForValue().set(SEND_ORDER_PREFIX+apiOrder.getId(),apiOrder);
        String finalMessageId = IdUtil.simpleUUID();
        rabbitTemplate.convertAndSend(order_exchange,"order-send",apiOrder, message -> {
            MessageProperties messageProperties = message.getMessageProperties();
            //生成全局唯一id
            messageProperties.setMessageId(finalMessageId);
            messageProperties.setContentEncoding("utf-8");
            return message;
        });
    }

    /**
     * 1、只要消息抵达服务器，那么b=true
     * @param correlationData 当前消息的唯一关联数据（消息的唯一id）
     * @param b 消息是否成功收到
     * @param s 失败的原因
     */
    @Override
    public void confirm(CorrelationData correlationData, boolean b, String s) {
        if (b){
            //投递成功则删除reids中的向mq发送订单的类容
            redisTemplate.delete(SEND_ORDER_PREFIX+finalId);
        }else {
            log.error("订单--消息投递到服务端失败：{}---->{}",correlationData,s);
        }
    }

    //注入
    @PostConstruct
    public void init(){
        rabbitTemplate.setConfirmCallback(this);
    }


    /**
     * 向mq发送订单消息
     * @param orderSn
     */
    public void sendOrderPaySuccess(String orderSn){
        finalId = Long.valueOf(orderSn);
        redisTemplate.opsForValue().set(RedisConstant.ORDER_PAY_SUCCESS_INFO+orderSn,orderSn);
        String finalMessageId = UUID.randomUUID().toString();
        rabbitTemplate.convertAndSend(RabbitMqConstant.order_exchange,"order.pay.success",orderSn, message -> {
            MessageProperties messageProperties = message.getMessageProperties();
            //生成全局唯一id
            messageProperties.setMessageId(finalMessageId);
            messageProperties.setContentEncoding("utf-8");
            return message;
        });
    }
}
