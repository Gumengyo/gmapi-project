package cn.jishuqin.thirdParty.listener;

import cn.jishuqin.common.constant.RabbitMqConstant;
import cn.jishuqin.common.model.dto.SmsDto;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Slf4j
@Component
public class SmsDelayListener {
    /**
     * 监听死信队列 - 记录发送短信失败后的日志
     * （可以记录日志，入库，人工干预处理）
     * @param sms
     * @param message
     * @param channel
     * @throws IOException
     */
    @RabbitListener(queues = RabbitMqConstant.sms_delay_queue)
    public void delayListener(SmsDto sms, Message message, Channel channel) throws IOException {
        try{
            log.error("监听到死信队列消息==>{}",sms);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        }catch (Exception e){
            channel.basicReject(message.getMessageProperties().getDeliveryTag(),true);
        }
    }
}
