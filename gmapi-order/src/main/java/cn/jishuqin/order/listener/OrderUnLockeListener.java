package cn.jishuqin.order.listener;

import cn.jishuqin.common.BaseResponse;
import cn.jishuqin.order.feign.UserFeignServices;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import cn.jishuqin.common.constant.RabbitMqConstant;
import cn.jishuqin.common.model.entity.ApiOrder;
import cn.jishuqin.common.model.entity.OrderLock;
import cn.jishuqin.common.model.vo.LockChargingVo;
import cn.jishuqin.common.service.InnerInterfaceInfoService;
import cn.jishuqin.order.service.ApiOrderService;
import cn.jishuqin.order.service.OrderLockService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;

/**
 * @author Gumeng
 */
@Slf4j
@Component
public class OrderUnLockeListener {

    @Resource
    private OrderLockService orderLockService;

    @Resource
    private ApiOrderService apiOrderService;

    @Autowired
    private UserFeignServices userFeignServices;

    /**
     * 监听死信队列 - 记录订单超市未支付的消息后的日志
     *
     * @param apiOrder
     * @param message
     * @param channel
     * @throws IOException
     */
    @RabbitListener(queues = RabbitMqConstant.order_delay_queue)
    public void delayListener(ApiOrder apiOrder, Message message, Channel channel) throws IOException {
        try {
            log.error("监听到死信队列消息==>{}", apiOrder);
            Long id = apiOrder.getId();
            ApiOrder byId = apiOrderService.getById(id);
            Long orderNum = apiOrder.getOrderNum();
            Long interfaceId = apiOrder.getInterfaceId();
            LockChargingVo lockChargingVo = new LockChargingVo(interfaceId,orderNum);
            if (null == byId) {
                // 库存已经扣了，但是出现异常，未能够生成订单和锁定订单结果
                BaseResponse<Boolean> response = userFeignServices.unlockAvailablePieces(lockChargingVo);
                boolean updated = response.getData();
                if (!updated) {
                    throw new RuntimeException();
                }
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            } else {
                if (byId.getStatus() == 0) {
                    // 订单未完成
                    String orderSn = byId.getOrderSn();
                    // 更新库存状态信息表
                    orderLockService.update(new UpdateWrapper<OrderLock>().eq("orderSn", orderSn).set("lockStatus", 0));
                    // 更新订单表状态
                    apiOrderService.update(new UpdateWrapper<ApiOrder>().eq("id", apiOrder.getId()).set("status", 2));
                    // 解锁库存
                    BaseResponse<Boolean> response = userFeignServices.unlockAvailablePieces(lockChargingVo);
                    boolean updated = response.getData();
                    if (!updated) {
                        throw new RuntimeException();
                    }
                }
            }
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
        }
    }
}
