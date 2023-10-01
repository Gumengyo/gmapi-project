package cn.jishuqin.order.listener;

import cn.jishuqin.common.BaseResponse;
import cn.jishuqin.order.feign.UserFeignServices;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import cn.jishuqin.common.ErrorCode;
import cn.jishuqin.common.constant.RabbitMqConstant;
import cn.jishuqin.common.constant.RedisConstant;
import cn.jishuqin.common.exception.BusinessException;
import cn.jishuqin.common.model.dto.LeftNumUpdateDto;
import cn.jishuqin.common.model.entity.ApiOrder;
import cn.jishuqin.common.model.entity.OrderLock;
import cn.jishuqin.common.service.InnerUserInterfaceInfoService;
import cn.jishuqin.order.service.ApiOrderService;
import cn.jishuqin.order.service.OrderLockService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 订单支付成功的监听
 * @author Gumeng
 */
@Slf4j
@Component
public class OrderPaySuccessListener {

    @Resource
    private OrderLockService orderLockService;

    @Resource
    private ApiOrderService apiOrderService;

    @Autowired
    private UserFeignServices userFeignServices;

    @Resource
    private RedisTemplate redisTemplate;

    @Transactional(rollbackFor = Exception.class)
    @RabbitListener(queues = RabbitMqConstant.order_pay_success)
    public void orderPaySuccessListener(String orderSn, Message message, Channel channel) throws IOException {
        try{
            String replace = orderSn.replace("\"", "");
            //消息抵达队列后，就进行删除操作
            redisTemplate.delete(RedisConstant.ORDER_PAY_SUCCESS_INFO + replace);
            // 解决重复投递问题
            Object o = redisTemplate.opsForValue().get(RedisConstant.ORDER_PAY_RABBITMQ + replace);
            if (null != o){
                //已经成功处理过了，直接放行
                channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
            }
            log.info("监听到《订单支付成功》的消息：{}",replace);
            if (null != replace){
                //修改订单和订单锁定状态，天然幂等
                orderLockService.orderPaySuccess(replace);
                apiOrderService.orderPaySuccess(replace);
                OrderLock orderLock = orderLockService.getOne(new QueryWrapper<OrderLock>().eq("orderSn", replace));
                ApiOrder order = apiOrderService.getOne(new QueryWrapper<ApiOrder>().eq("orderSn", replace));
                Long lockNum = orderLock.getLockNum();
                Long userId = order.getUserId();
                Long interfaceId = order.getInterfaceId();
                LeftNumUpdateDto leftNumUpdateDto = new LeftNumUpdateDto(userId, interfaceId, lockNum);
                // 远程调用，增加用户剩余调用次数
                BaseResponse<Boolean> response = userFeignServices.updateUserLeftNum(leftNumUpdateDto);
                Boolean updated = response.getData();
                if (!updated){
                    throw new BusinessException(ErrorCode.OPERATION_ERROR);
                }
            }
            redisTemplate.opsForValue().set(RedisConstant.ORDER_PAY_RABBITMQ + replace,"true",30, TimeUnit.MINUTES);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        }catch (Exception e){
            log.error("listener error: {}",e.getMessage());
            redisTemplate.delete(RedisConstant.ORDER_PAY_RABBITMQ +orderSn.replace("\"", ""));
            channel.basicReject(message.getMessageProperties().getDeliveryTag(),true);
        }
    }
}
