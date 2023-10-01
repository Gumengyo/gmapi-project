package cn.jishuqin.thirdParty.listener;

import cn.jishuqin.common.constant.RabbitMqConstant;
import cn.jishuqin.common.model.dto.SmsDto;
import cn.jishuqin.thirdParty.utils.AliyunSmsUtils;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;

/**
 * @author gumeng
 * 发送短信验证码
 */
@Slf4j
@Component
public class SendSmsListener {

    @Resource
    private AliyunSmsUtils aliyunSmsUtils;

    @Resource
    private RedisTemplate redisTemplate;


    /**
     * 监听普通队列 - 实际发送短信
     * 出现异常，使用消息重传。
     * @param sms
     * @param message
     * @param channel
     * @throws IOException
     */
    @RabbitListener(queues = RabbitMqConstant.sms_queue)
    public void listener(SmsDto sms, Message message, Channel channel) throws IOException {
        String messageId = message.getMessageProperties().getMessageId();
        int retryCount = (int) redisTemplate.opsForHash().get(RabbitMqConstant.SMS_HASH_PREFIX+messageId, "retryCount");
        if (retryCount >= 3){
            //投递次数大于三次，放入死信队列
            log.error("重试次数大于三次");
            channel.basicReject(message.getMessageProperties().getDeliveryTag(),false);
            redisTemplate.delete(RabbitMqConstant.SMS_HASH_PREFIX + messageId);
            return;
        }
        try{
            String phone = sms.getPhone();
            String code = sms.getCode();
            if (null == phone || null == code){
                throw new RuntimeException("请求参数错误");
            }
            //发送验证码
            SendSmsResponse response = aliyunSmsUtils.sendMessage(phone, code);
            String result = response.getBody().getCode();
            Integer statusCode = response.getStatusCode();
            if (!"OK".equals(result) || statusCode != 200){
                throw new RuntimeException("发送验证码失败");
            }
            //手动确认消息
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
            log.info("发送短信成功--{}",sms);
            //发送成功后，从redis中删除该缓存
            redisTemplate.delete(RabbitMqConstant.SMS_HASH_PREFIX + messageId);
        }catch (Exception e){
            //进行重试，重试次数加1
            redisTemplate.opsForHash().put(RabbitMqConstant.SMS_HASH_PREFIX+messageId,"retryCount",retryCount+1);
            channel.basicReject(message.getMessageProperties().getDeliveryTag(),true);
        }
    }


}
